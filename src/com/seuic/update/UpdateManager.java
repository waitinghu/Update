package com.seuic.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;





import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler.Callback;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class UpdateManager implements Callback{
    
    private static UpdateManager manager;
    private Context mContext;
    private Handler handler;
    
    private static final String XML_URL = "http://172.27.35.1:8080/examples/update.xml";
    private static final String APK_URL = "http://172.27.35.1:8080/examples/";
    
    public static final int MSG_DOWNLOAD_XML_FAIL = 1;
    public static final int MSG_DOWNLOAD_XML_SUCCESS = 2;
    public static final int MSG_CHECK_UPDATE_CANCEL = 3;
    public static final int MSG_DOWNLOAD_APK = 4;
    public static final int MSG_NO_SDCARD = 5;
    public static final int MSG_APK_EXISTS = 6;
    public static final int MSG_DOWNLOAD_OVER = 7;
    public static final int MSG_DOWNLOADING = 8;
    public static final int MSG_DOWNLOAD_APK_CANCEL = 9;
    
    private ProgressDialog mCheckDialog;
    private ProgressDialog mDownLoadDialog;
    private Thread mDownloadXMLThread;
    private Thread mDownLoadAPKThread;
    private UpdateInfo info;
    
    private boolean UIInterceptFlag = false;
    private int mDownloadProcess ;
    
    private UpdateManager(Context context){
        this.mContext = context;
        handler = new Handler(mContext.getMainLooper(), this);
    };
    
    public static synchronized UpdateManager getInstance(Context context) {
        if(manager == null) {
            manager = new UpdateManager(context);
        }
        return manager;
    }
    
    public int getCurrentVersion() {
        int curVersionCode = -1;
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            curVersionCode = info.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return curVersionCode;
    }
    
    public void showCheckUpdateDailog() {
        UIInterceptFlag = false;
        mDownloadProcess = 0;
        Message msg = new Message();
        mCheckDialog = new ProgressDialog(mContext);
        mCheckDialog.setTitle("版本更新");
        mCheckDialog.setMessage("正在检测更新请稍等...");
        mCheckDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mCheckDialog.setCancelable(false);
        msg.what = MSG_CHECK_UPDATE_CANCEL;
        mCheckDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",msg);
        mCheckDialog.show();
        loadUpdateInfo();
    }
    
    public void showNoticeDialog() {
        
        String tile = "软件版本更新";
        String massage = "暂无版本更新...";
        String negativeButtonText = "确定";
        int serverVerson = info.getVersion();
        boolean canUpdate = getCurrentVersion() < serverVerson;
        
        if(canUpdate){
            ArrayList<String> description = info.getDescription();
            StringBuilder sb = new StringBuilder();
            sb.append("发现新版本," +"大小：" +info.getSize() + "\n");
            sb.append("更新内容：" +"\n");
            for(String s : description) {
                sb.append(s);
                sb.append("\n");
            }
            massage = sb.toString();
            negativeButtonText = "以后再说";
        }
        
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(tile);
        builder.setMessage(massage);
        if(canUpdate) {
            builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    handler.sendEmptyMessage(MSG_DOWNLOAD_APK);
                }
            });
        }
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog noticeDialog = builder.create();
        noticeDialog.show();
    }
    
    
    private void downloadApk() {
        mDownLoadAPKThread = new Thread(mDownApkRunnable);
        mDownLoadAPKThread.start();
    }
    
    private Runnable mDownApkRunnable = new Runnable() {
        
        @Override
        public void run() {
            
            InputStream is = null;
            FileOutputStream fos =  null;
            
            try {
                
                String apkName = info.getName();
                String tmpApk = apkName + info.getVersion()+ ".tmp";
                
                String apkFilePath = null;
                String tmpFilePath = null;
                
                // 判断是否挂载了SD卡
                String storageState = Environment.getExternalStorageState();
                if (storageState.equals(Environment.MEDIA_MOUNTED)) {
                    String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Update/";
                    File file = new File(savePath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    apkFilePath = savePath + apkName;
                    tmpFilePath = savePath + tmpApk;
                }

                // 没有挂载SD卡，无法下载文件
                if (apkFilePath == null || apkFilePath == "") {
                    handler.sendEmptyMessage(MSG_NO_SDCARD);
                    return;
                }

                File ApkFile = new File(apkFilePath);

                // 是否已下载更新文件
                if (ApkFile.exists()) {
                    Message message = new Message();
                    message.obj = ApkFile.toString();
                    message.what = MSG_APK_EXISTS;
                    handler.sendMessage(message);
                    return;
                }
                // 输出临时下载文件
                File tmpFile = new File(tmpFilePath);
                fos = new FileOutputStream(tmpFile);
                URL url = new URL(APK_URL+apkName);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                int length = conn.getContentLength();
                is = conn.getInputStream();
                // 显示文件大小格式：2个小数点显示
                DecimalFormat df = new DecimalFormat("0.00");
                // 进度条下面显示的总文件大小
//                apkFileSize = df.format((float) length / 1024 / 1024) + "MB";
                
                int count = 0;
                byte buf[] = new byte[1024];
                
                do {
                    int numread = is.read(buf);
                    count += numread;
                    
                    // 进度条下面显示的当前下载文件大小
                    String tmpFileSize = df.format((float) count / 1024 / 1024) + "MB";
                    
                    // 当前进度值
                    mDownloadProcess = (int) (((float) count / length) * 100);
                    
                    // 更新进度
                    handler.sendEmptyMessage(MSG_DOWNLOADING);
                    
                    if (numread <= 0) {
                        // 下载完成 -将临时下载文件转成APK文件
                        if (tmpFile.renameTo(ApkFile)) {
                            // 通知安装
                            Message message = new Message();
                            message.obj = ApkFile.toString();
                            message.what = MSG_DOWNLOAD_OVER;
                            handler.sendMessage(message);
                        }
                        break;
                    }
                    fos.write(buf, 0, numread);
                } while (!UIInterceptFlag); //点击取消就停止下载

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    
    
    private void showDownloadDialog() {
        Message msg = new Message();
        msg.what = MSG_DOWNLOAD_APK_CANCEL;
        mDownLoadDialog = new ProgressDialog(mContext);
        mDownLoadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); 
        mDownLoadDialog.setTitle("正在下载新版本...");  
        mDownLoadDialog.setProgress(100); 
        mDownLoadDialog.setIndeterminate(false);
        mDownLoadDialog.setCancelable(false);
        mDownLoadDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", msg);
        mDownLoadDialog.show();
        downloadApk();
    }
    
    
    public void loadUpdateInfo() {
        mDownloadXMLThread = new Thread(DownLoadXMLRunnable);
        mDownloadXMLThread.start();
    }
    
    public Runnable DownLoadXMLRunnable =  new Runnable() {
        public void run() {
            UpdateInfo info = null;
            try {
                HttpURLConnection conn=(HttpURLConnection)new URL(XML_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                if (conn.getResponseCode()==200) {
                    InputStream inputStream=conn.getInputStream();
                    info =DownloadXMLParser.parse(inputStream);
                    Message msg = new Message();
                    msg.obj = info;
                    msg.what = MSG_DOWNLOAD_XML_SUCCESS;
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Message msg = new Message();
                msg.what = MSG_DOWNLOAD_XML_FAIL;
                handler.sendMessage(msg);
            }
        }
    };
    
    
    private void installApk(String path) {
        if(path != null) {
            File apkfile = new File(path);
            if (!apkfile.exists()) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + apkfile.toString()),"application/vnd.android.package-archive");
            mContext.startActivity(intent);
        }
    }
    
    /**
     * public static final int MSG_DOWNLOAD_XML_FAIL = 1;
      * public static final int MSG_DOWNLOAD_XML_SUCCESS = 2;
      * public static final int MSG_CHECK_UPDATE_CANCEL = 3;
      * public static final int MSG_DOWNLOAD_APK = 4;
      * public static final int MSG_NO_SDCARD = 5;
      * public static final int MSG_APK_EXISTS = 6;
      * public static final int MSG_DOWNLOAD_OVER = 7;
      * public static final int MSG_DOWNLOADING = 8;
     */
    @Override
    public boolean handleMessage(Message msg) {
       int what = msg.what;
       
       switch (what) {
       
        case MSG_DOWNLOAD_XML_SUCCESS:
        info = (UpdateInfo) msg.obj;
            if(mCheckDialog != null) {
                mCheckDialog.dismiss();
            }
            showNoticeDialog();
            break;
            
        case MSG_CHECK_UPDATE_CANCEL:
            if(mCheckDialog != null) {
                mCheckDialog.dismiss();
            }
            if(mDownloadXMLThread != null) {
                mDownloadXMLThread.interrupt();
                mDownloadXMLThread = null;
            }
            
        case MSG_DOWNLOAD_XML_FAIL:
            if(mCheckDialog != null) {
                mCheckDialog.dismiss();
            }
            break;
            
        case MSG_DOWNLOAD_APK:
            showDownloadDialog();
            break;
            
        case MSG_DOWNLOADING:
            mDownLoadDialog.setProgress(mDownloadProcess);
            break;
        case MSG_APK_EXISTS:
        case MSG_DOWNLOAD_OVER:
            mDownLoadDialog.dismiss();
            String path = (String) msg.obj;
            installApk(path);
            break;
        case MSG_NO_SDCARD:
            mDownLoadDialog.dismiss();
            Toast.makeText(mContext, "无SD卡", Toast.LENGTH_SHORT).show();
            break;
        case MSG_DOWNLOAD_APK_CANCEL:
            UIInterceptFlag = true;
            break;
        default:
            break;
        }
       
        return true;
    }
}
