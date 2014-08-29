package com.joshtttt.jtlibrary.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.joshtttt.jtlibrary.util.Log;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class Samba {
	public final static String TAG = "Samba";
	
	/**
	 * 从局域网中共享文件中得到文件并保存在本地磁盘上
	 * @param remoteUrl 共享电脑路径 如：smb//administrator:123456@172.16.10.136/smb/1221.zip  , smb为共享文件
	 * 注：如果一直出现连接不上，有提示报错，并且错误信息是 用户名活密码错误 则修改共享机器的文件夹选项 查看 去掉共享简单文件夹的对勾即可。
	 * @param localDir 本地路径 如：D:/
	 */
	public static void download(String remoteUrl,String localDir){
		InputStream in = null;
		OutputStream out = null;
		try {
			SmbFile smbFile = new SmbFile(remoteUrl);
			String fileName = smbFile.getName();
			File localFile = new File(localDir+File.separator+fileName);
			in = new BufferedInputStream(new SmbFileInputStream(smbFile));
			out = new BufferedOutputStream(new FileOutputStream(localFile));
			byte []buffer = new byte[1024];
			while((in.read(buffer)) != -1){
				out.write(buffer);
				buffer = new byte[1024];
			}
		} catch (Exception e) {
			Log.t(TAG, e);
		}finally{
			try {
				out.close();
				in.close();
			} catch (IOException e) {
				Log.t(TAG, e);
			}
		}
	}
	/**
	 * 把本地磁盘中的文件上传到局域网共享文件下
	 * @param remoteUrl 共享电脑路径 如：smb//administrator:123456@172.16.10.136/smb
	 * @param localFilePath 本地路径 如：D:/
	 */
	public static void upload(String remoteUrl,String localFilePath){
		InputStream in = null;
		OutputStream out = null;
		try {
			File localFile = new File(localFilePath);
			String fileName = localFile.getName();
			SmbFile remoteFile = new SmbFile(remoteUrl+"_"+fileName);
			in = new BufferedInputStream(new FileInputStream(localFile));
			out = new BufferedOutputStream(new SmbFileOutputStream(remoteFile));
			byte []buffer = new byte[1024];
			while((in.read(buffer)) != -1){
				out.write(buffer);
				buffer = new byte[1024];
			}
		} catch (Exception e) {
			Log.t(TAG, e);
		}finally{
			try {
				if (out != null) out.close();
				if (in != null) in.close();
			} catch (IOException e) {
				Log.t(TAG, e);
			}
		}
	}
}

