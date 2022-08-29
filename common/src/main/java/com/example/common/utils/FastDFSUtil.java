package com.example.common.utils;

import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

public class FastDFSUtil {
    /**
     * 加载Tracker链接信息
     */
    static {
        try {
            //查找classpath下的文件路径
            String filename = new ClassPathResource("fastdfs.conf").getPath();
            ClientGlobal.init(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //文件上传，保存在linux系统的/storage/files路径中
    public static String[] upload(String uploadUrl) {
        TrackerServer ts = null;
        StorageServer ss = null;
        String[] ret = null;
        try {
            TrackerClient tc = new TrackerClient();
            ts = tc.getTrackerServer();
            ss = tc.getStoreStorage(ts);
            StorageClient sc = new StorageClient(ts,ss);
            /**
             * 参数1 本地文件绝对路径
             * 参数2 文件后缀
             * 参数3 文件的属性文件，通常不上传
             * 返回值包含了组名和文件的远程路径名，建议存入数据库方便以后下载和查看
             * uploadUrl: "C:/Users/liyuan/Pictures/Saved Pictures/怒之铁拳4.jpg"
             */
            String[] strings = uploadUrl.split("\\.");
            if (strings != null && strings.length>0) {
                ret = sc.upload_file(uploadUrl, strings[strings.length - 1], null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return ret;
    }
    //文件下载
    public static int download(String groupName,String remoteUrl,String fileName) {
        TrackerServer ts = null;
        StorageServer ss = null;
        int ret = -1;
        try {
            TrackerClient tc = new TrackerClient();
            ts = tc.getTrackerServer();
            ss = tc.getStoreStorage(ts);
            StorageClient sc = new StorageClient(ts,ss);
            /**
             * 参数1 组名
             * 参数2 文件的远程路径名
             * 参数3 下载到本地的文件名
             * 返回值 0表示下载成功
             */
            ret = sc.download_file(groupName, remoteUrl, fileName);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return ret;
    }
    //文件删除
    public static int delete(String groupName,String remoteUrl) {
        TrackerServer ts = null;
        StorageServer ss = null;
        int ret = -1;
        try {
            TrackerClient tc = new TrackerClient();
            ts = tc.getTrackerServer();
            ss = tc.getStoreStorage(ts);
            StorageClient sc = new StorageClient(ts, ss);
            /**
             * 参数1 组名("group1")
             * 参数2 文件的远程路径名("M00/00/00/wKgMgGMLSzOACYreAAepcYyY2uQ993.jpg")
             * 返回值 0表示删除成功
             */
            ret = sc.delete_file(groupName, remoteUrl);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return ret;
    }

}
