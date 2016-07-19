package de.qabel.qabelbox.box.views

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.cocosw.bottomsheet.BottomSheet
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.adapters.FileAdapter
import de.qabel.qabelbox.box.dagger.modules.FileBrowserModule
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import de.qabel.qabelbox.box.provider.BoxProvider
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import kotlinx.android.synthetic.main.fragment_files.*
import org.jetbrains.anko.*
import java.net.URLConnection
import javax.inject.Inject

class FileBrowserFragment: FileBrowserView, BaseFragment(), AnkoLogger,
        SwipeRefreshLayout.OnRefreshListener {

    companion object {
        fun newInstance() = FileBrowserFragment()
    }

    override val isFabNeeded = true

    override val title: String by lazy { ctx.getString(R.string.filebrowser) }

    @Inject
    lateinit var presenter: FileBrowserPresenter

    lateinit var adapter: FileAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java)
                .plus(FileBrowserModule(this))
        component.inject(this)
        setHasOptionsMenu(true)
        adapter = FileAdapter(mutableListOf(),
                click = { presenter.onClick(it) }, longClick = { openBottomSheet(it)}
        )

        files_list.layoutManager = LinearLayoutManager(ctx)
        files_list.adapter = adapter
    }

    fun openBottomSheet(entry: BrowserEntry) {
        val (icon, sheet) = when(entry) {
            is BrowserEntry.File -> Pair(R.drawable.file, R.menu.bottom_sheet_files)
            is BrowserEntry.Folder -> Pair(R.drawable.folder, R.menu.bottom_sheet_folder)
        }
        BottomSheet.Builder(activity).title(entry.name).icon(icon).sheet(sheet).listener {
            dialogInterface, menu_id ->
            when(entry) {
                is BrowserEntry.File -> when (menu_id) {
                    R.id.share -> presenter.share(entry)
                    R.id.delete -> presenter.delete(entry)
                    R.id.export -> presenter.export(entry)
                }
                is BrowserEntry.Folder -> when (menu_id) {
                    R.id.delete -> presenter.deleteFolder(entry)
                }
            }
        }.show()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_files, container, false)
                ?: throw IllegalStateException("Could not create view")
    }



    override fun showEntries(entries: List<BrowserEntry>) {
        onUiThread {
            adapter.entries.clear()
            adapter.entries.addAll(entries)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onRefresh() {
        presenter.onRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.ab_files, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.menu_refresh) {
            onRefresh()
            return true
        }
        return false
    }

    override fun open(documentId: DocumentId) {
        val uri = DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY,
                documentId.toString())
        val type = URLConnection.guessContentTypeFromName(uri.toString())
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, type)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Open with").singleTop())
    }
    override fun export(documentId: DocumentId) {
        TODO()
    }

    override fun handleFABAction(): Boolean {
        BottomSheet.Builder(activity).sheet(R.menu.bottom_sheet_files_add).listener { dialog, which ->
            when (which) {
                R.id.upload_file -> {
                }
                R.id.create_folder -> {
                    createFolderDialog()
                }
            }
        }.show()
        return true
    }

    private fun createFolderDialog() {
        UI {
            alert(R.string.create_folder) {
                customView {
                    verticalLayout {
                        val folderName = editText {
                            hint = ctx.getString(R.string.add_folder_name)
                        }
                        positiveButton(R.string.ok) {
                            presenter.createFolder(
                                    BrowserEntry.Folder(folderName.text.toString()))
                        }
                        negativeButton(R.string.cancel) { }
                    }
                }
            }.show()
        }
    }


}

