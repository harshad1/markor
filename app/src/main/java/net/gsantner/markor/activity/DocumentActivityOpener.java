package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import net.gsantner.markor.util.DocumentIO;

import java.io.File;
import java.io.Serializable;

/**
 * This Activity exists solely to launch DocumentActivity with the correct intent
 * it is necessary as widget and shortcut intents don't work quite correctly
 */
public class DocumentActivityOpener extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchDocumentActivityAndFinish(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        launchDocumentActivityAndFinish(intent);
    }

    private void launchDocumentActivityAndFinish(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_SEND.equals(action)) {
            Serializable path = intent.getSerializableExtra(DocumentIO.EXTRA_PATH);
            boolean isFolder = intent.getBooleanExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
            if (path instanceof File) {
                Intent docIntent = DocumentActivity.makeIntent(this, (File) path, isFolder)
                        .setAction(action);
                startActivity(docIntent);
            }
        }
        finish();
    }

    public static Intent makeIntent(Context context, String action, File path, boolean isFolder) {
        return new Intent(context, DocumentActivityOpener.class)
                .setAction(action)
                .putExtra(DocumentIO.EXTRA_PATH, path)
                .putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, isFolder);
    }
}