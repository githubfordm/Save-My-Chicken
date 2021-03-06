package droidninja.filepicker.cursors;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import droidninja.filepicker.PickerManager;
import droidninja.filepicker.cursors.loadercallbacks.FileResultCallback;
import droidninja.filepicker.models.Document;
import droidninja.filepicker.models.FileType;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.MediaColumns.DATA;

/**
 * Created by hoon on 2017-06-06.
 */

public class PickerScannerTask extends AsyncTask<Void,Void,List<Document>> {

    private String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PicKeep/";
    private final String tag = "FileScanner";

    final String[] DOC_PROJECTION = {
            MediaStore.Images.Media._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Files.FileColumns.TITLE
    };
    private final FileResultCallback<Document> resultCallback;
    private final Context context;
    private File file;

    public PickerScannerTask(Context context, FileResultCallback<Document> fileResultCallback)
    {
        this.context = context;
        this.resultCallback = fileResultCallback;
    }

    @Override
    protected List<Document> doInBackground(Void... voids) {
        ArrayList<Document> documents = new ArrayList<>();

        final String[] projection = DOC_PROJECTION;
        String selection = MediaStore.Files.FileColumns.DATA+" like ? ";  // 이미지,비디오같은 파일을 제외한 파일만 select하겠다는 으의지

        final Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                new String[]{dir+"%"},
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

        if(cursor!=null) {
            documents = getDocumentFromCursor(cursor);
            cursor.close();
        }

        return documents;
    }

    @Override
    protected void onPostExecute(List<Document> documents) {
        super.onPostExecute(documents);
        if (resultCallback != null) {
            resultCallback.onResultCallback(documents);
        }
    }

    private ArrayList<Document> getDocumentFromCursor(Cursor data)
    {
        ArrayList<Document> documents = new ArrayList<>();

        while (data.moveToNext()) {

            int imageId  = data.getInt(data.getColumnIndexOrThrow(_ID));
            String path = data.getString(data.getColumnIndexOrThrow(DATA));
            String title = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE));

            if(path!=null) {

                FileType fileType = getFileType(PickerManager.getInstance().getFileTypes(),path);  // 무수히 많은 data의 path경로에서 마지막부분이 어떤 형식(extension)으로 끝나는지 체크해서 해당 파일타입을 리턴한다.
                if(fileType!=null && !(new File(path).isDirectory())) { // 파일타입이 null이 아니고 해당 경로 파일이 폴더가 아니라면(즉, 실제 파일 = 문서라면)

                    Document document = new Document(imageId, title, path); // 각 문서 파일의 정보를 담는 Document 객체를 생성하고
                    document.setFileType(fileType);     // 이 객체는 어떤 extension 파일타입인지에 대한 정보도 설정해놓는다.

                    String mimeType = data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE));
                    if (mimeType != null && !TextUtils.isEmpty(mimeType))   // 이건 왜 하는지 이해안감
                        document.setMimeType(mimeType);
                    else {
                        document.setMimeType("");
                    }

                    document.setSize(data.getString(data.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)));    // 현재 경로의 파일의 크기의 정보또한 저장해놓는다.

                    if (!documents.contains(document))  // 혹시 데이터베이스 검색결과 파일이 이전에 받은 파일이랑 중복되는 경우가 있을수도 있으니 체크
                        documents.add(document);
                }
            }
        }

        return documents;   // 최종적으로 모든 파일들을 문서화 정보로 변환한 객체의 리스트를 리턴한다.
    }

    private FileType getFileType(ArrayList<FileType> types, String path) {
        for (int index = 0; index < types.size(); index++) {
            for (String string : types.get(index).extensions) {
                if (path.endsWith(string))
                    return types.get(index);
            }
        }
        return null;
    }
}
