package de.qabel.core.exceptions;

public class QblStorageNameConflict extends QblStorageException {
	public QblStorageNameConflict(Throwable e) {
		super(e);
	}

	public QblStorageNameConflict(String s) {
		super(s);
	}
}
