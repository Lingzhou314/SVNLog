package com.zhoulz;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * svnlog
 * 
 * @author zhoulz
 * @createBy 2017年10月13日
 */
public class SVNLog {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String url = "svn://127.0.0.1/source/p2p";
		String username = "123";
		String password = "123";
		String target = "D:/java/workspace/p2p/target";
		String srcJava = "src/main/java";
		String srcResource = "src/main/resources";
		String webapp = "src/main/webapp";
		String svnDir = "/source/p2p";
		String projectName = "p2p";
		long startRevision = 5678;
		long endRevision = 6596;
		
		SVNLog svnLog = new SVNLog(url, username, password);
		svnLog.createMavenProjectPatch(target, srcJava, srcResource, webapp, svnDir, projectName, startRevision, endRevision);
	}

	private String url;
	private String username;
	private String password;
	private SVNRepository repository;

	public SVNLog(String url, String username, String password) throws SVNException {
		this.url = url;
		this.username = username;
		this.password = password;
		// 版本库初始化
		DAVRepositoryFactory.setup();
		repository = DAVRepositoryFactory.create(SVNURL.parseURIEncoded(url));
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
		repository.setAuthenticationManager(authManager);
	}

	/**
	 * 打maven项目svn增量包，删除文件需要手动执行删除，最终生成增加文件到${project}/target/update目录
	 * 
	 * @param target 编译目标文件夹: ${project}/target
	 * @param srcJava src/main/java
	 * @param srcResource src/main/resources
	 * @param webapp src/main/webapp
	 * @param svnDir svn项目文件夹目录：/trunk/p2p
	 * @param projectName 项目名： p2p
	 * @param startRevision 起始版本号（含）
	 * @param endRevision 结束版本号（含）
	 */
	public void createMavenProjectPatch(String target, String srcJava, String srcResource, String webapp, String svnDir, String projectName, long startRevision, long endRevision) {
		List<SVNLogEntry> logs = this.getSVNLogs(startRevision, endRevision);
		String updateDir = target + "/update";
		File updateFileDir = new File(updateDir);
		updateFileDir.deleteOnExit();
		for (SVNLogEntry svnLog : logs) {
			Map<String, SVNLogEntryPath> myChangedPaths = svnLog.getChangedPaths();
			for (Iterator<SVNLogEntryPath> paths = myChangedPaths.values().iterator(); paths.hasNext();) {
				SVNLogEntryPath path = paths.next();
				// 提交类型
				char pathType = path.getType();
				switch (pathType) {
				case SVNLogEntryPath.TYPE_DELETED:
					System.out.println("删除：" + path.getPath());
					break;
				case SVNLogEntryPath.TYPE_ADDED:
				case SVNLogEntryPath.TYPE_MODIFIED:
				case SVNLogEntryPath.TYPE_REPLACED:
					if (path.getKind().equals(SVNNodeKind.FILE)) {
						String filePath = path.getPath().substring(svnDir.length() + 1);
						String classPath = "WEB-INF/classes";
						if (filePath.startsWith(srcJava)) {
							filePath = classPath + filePath.substring(srcJava.length());
							filePath = filePath.replaceAll("java$", "class");
						} else if (filePath.startsWith(srcResource)) {
							filePath = classPath + filePath.substring(srcResource.length());
						} else if (filePath.startsWith(webapp)) {
							filePath = filePath.substring(webapp.length() + 1);
						} else {
							System.err.println("非打包文件：" + path);
							continue;
						}
						copyFile(target + "/" + projectName + "/" + filePath, updateDir + "/" + projectName + "/" + filePath);
					} else {
						System.err.println("非文件格式：" + path);
					}
					break;

				default:
					break;
				}
			}
		}
	}

	public List<SVNLogEntry> getSVNLogs(long startRevision, long endRevision) {
		try {
			// 存放结果
			List<SVNLogEntry> logEntries = new ArrayList<SVNLogEntry>();
			repository.log(new String[] { "/" }, logEntries, startRevision, endRevision, true, true);
			return logEntries;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static void copyFile(String sourceFileNameStr, String desFileNameStr) {
		System.out.println("copyTo " + desFileNameStr);
		File srcFile = new File(sourceFileNameStr);
		File desFile = new File(desFileNameStr);
		try {
			copyFile(srcFile, desFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void copyFile(File sourceFile, File targetFile) throws IOException {
		BufferedInputStream inBuff = null;
		BufferedOutputStream outBuff = null;
		try {
			if (!sourceFile.exists()) {
				System.err.println("源文件不存在：" + sourceFile.getAbsolutePath());
				return;
			}
			// 新建文件输入流并对它进行缓冲
			inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

			if (!targetFile.getParentFile().exists()) {
				targetFile.getParentFile().mkdirs();
			}
			// 新建文件输出流并对它进行缓冲
			outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

			// 缓冲数组
			byte[] b = new byte[1024 * 5];
			int len;
			while ((len = inBuff.read(b)) != -1) {
				outBuff.write(b, 0, len);
			}
			// 刷新此缓冲的输出流
			outBuff.flush();
		} finally {
			// 关闭流
			if (inBuff != null)
				inBuff.close();
			if (outBuff != null)
				outBuff.close();
		}
	}

}
