package de.qabel.qabelbox.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.BoxAccountRegisterServer;
import de.qabel.qabelbox.communication.callbacks.JsonRequestCallback;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.UIHelper;
import okhttp3.Response;

public class CreateAccountLoginFragment extends BaseIdentityFragment {

    private EditText etPassword;
    private TextView etUserName;

    private final BoxAccountRegisterServer mBoxAccountServer = new BoxAccountRegisterServer();
    private View resetPassword;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_create_account_login, container, false);
        etUserName = ((TextView) view.findViewById(R.id.et_username));
        etPassword = (EditText) view.findViewById(R.id.et_password);
        resetPassword = view.findViewById(R.id.reset_password);

        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getFragmentManager().popBackStack();
                CreateAccountResetPasswordFragment fragment = new CreateAccountResetPasswordFragment();
                getActivity().getFragmentManager().beginTransaction().replace(R.id.fragment_container_content, fragment).addToBackStack(null).commit();
            }
        });
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.ab_next, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_ok) {
            String check = checkData();
            if (check != null) {
                UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, check);
                return true;
            }
            login(etUserName.getText().toString(), etPassword.getText().toString());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String checkData() {

        if (etPassword.getText().toString().length() < 3 || etUserName.getText().toString().length() == 0) {
            return getString(R.string.create_account_enter_all_data);
        }
        return null;
    }

    private void login(final String username, final String password) {
        final AlertDialog dialog = UIHelper.showWaitMessage(mActivity, R.string.dialog_headline_please_wait, R.string.dialog_message_server_communication_is_running, false);
        final JsonRequestCallback callback = createCallback(username, dialog);
        mBoxAccountServer.login(username, password, callback);
    }

    @NonNull
    private JsonRequestCallback createCallback(final String username, final AlertDialog dialog) {

        return new JsonRequestCallback(new int[]{200, 400, 429}) {

            @Override
            protected void onError(Exception e, @Nullable Response response) {
                dialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), R.string.server_access_failed_or_invalid_check_internet_connection, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            protected void onJSONSuccess(Response response, JSONObject json) {
                BoxAccountRegisterServer.ServerResponse result = BoxAccountRegisterServer.parseJson(json);
                if (result.token != null && result.token.length() > 5) {
                    AppPreference appPrefs = new AppPreference(getActivity());
                    appPrefs.setToken(result.token);
                    appPrefs.setAccountName(username);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            mActivity.completeWizard();
                        }
                    });
                } else {
                    String text = generateErrorMessage(result);
                    dialog.dismiss();
                    UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, text);
                }
            }

            private String generateErrorMessage(BoxAccountRegisterServer.ServerResponse result) {

                ArrayList<String> message = new ArrayList<>();
                if (result.non_field_errors != null) {
                    message.add(result.non_field_errors);
                }
                if (result.password != null) {
                    message.add(result.password);
                }
                if (result.username != null) {
                    message.add(result.username);
                }

                String errorText = "";
                if (message.size() == 0) {
                    errorText = getString(R.string.server_access_failed_or_invalid_check_internet_connection);
                } else {
                    errorText = "- " + message.get(0);
                    for (int i = 1; i < message.size(); i++) {
                        errorText += "\n -" + message.get(i);
                    }
                }
                return errorText;
            }
        };
    }

    @Override
    public String check() {

        UIHelper.showDialogMessage(getActivity(), R.string.dialog_headline_info, R.string.function_not_yet_implenented);
        //return mChecker.check(editText);
        return null;
    }
}
