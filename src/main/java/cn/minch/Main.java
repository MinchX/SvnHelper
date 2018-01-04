package cn.minch;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Minch
 * Date: 2018/1/3
 * Time: 14:55
 */
public class Main {

    public static void main(String[] args) throws IOException {

        //起始版本号
        long startRevision = 1297;
        //终止版本号 -1表示到最新
        long endRevision = -1;

        //svn地址
        String svnUrl = "svn://xxx";
        String username = "xxx";
        String password = "xxx";
//        复制的文件目录
        String targetPath = "D:\\ideaProject\\qixie\\target\\qixie-1.0-SNAPSHOT";
//        svn目录中需要替换的部分
        String svnCutHead = "/qixie/src/main/";
//        最终文件存放的目录
        String copyPath = "D:\\updatePath\\1";

        try {
            //清空文件存放目录
            File file = new File(copyPath);
            if (file.exists()) {
                deleteDir(file);
            }
            SVNRepositoryFactoryImpl.setup();

            SVNURL url = null;

            url = SVNURL.parseURIEncoded(svnUrl);
            SVNRepository repository = SVNRepositoryFactory.create(url,null);

            BasicAuthenticationManager basicAuthenticationManager = new BasicAuthenticationManager(username,password);

            repository.setAuthenticationManager(basicAuthenticationManager);



            Collection logEntries = null;

            logEntries = repository.log(new String[]{""},null,startRevision,endRevision,true,true);


            for (Iterator entries = logEntries.iterator(); entries.hasNext(); ) {
                SVNLogEntry logEntry = ( SVNLogEntry ) entries.next();
                System.out.println( "---------------------------------------------" );
                System.out.println ("revision: " + logEntry.getRevision() );
                System.out.println( "author: " + logEntry.getAuthor() );
                System.out.println( "date: " + logEntry.getDate() );
                System.out.println( "log message: " + logEntry.getMessage() );

                if ( logEntry.getChangedPaths().size() > 0 ) {
                    System.out.println();
                    System.out.println( "changed paths:" );
                    Set changedPathsSet = logEntry.getChangedPaths().keySet();

                    for ( Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext(); ) {
                        SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get( changedPaths.next() );
                        System.out.println( " "
                                + entryPath.getType()
                                + " "
                                + entryPath.getPath()
                                + ( ( entryPath.getCopyPath() != null ) ? " (from "
                                + entryPath.getCopyPath() + " revision "
                                + entryPath.getCopyRevision() + ")" : "" ) );

                        if (entryPath.getType()=='D'){
                            continue;
                        }

                        String filePath = entryPath.getPath();

                        File sourceFile = null;
                        File targetFile = null;

                        if (filePath.startsWith(svnCutHead+"java/")){
                            filePath = filePath.replace(svnCutHead+"java/","");
                            if (filePath.endsWith(".java")){
                                filePath = filePath.replace(".java",".class");
                            }
                            sourceFile = new File(targetPath+"/WEB-INF/classes/"+filePath);
                            targetFile = new File(copyPath+"/WEB-INF/classes/"+filePath);

                        }else if (filePath.startsWith(svnCutHead+"webapp/")){
                            filePath = filePath.replace(svnCutHead+"webapp/","");
                            sourceFile = new File(targetPath+"/"+filePath);
                            targetFile = new File(copyPath+"/"+filePath);
                        }else {
                            filePath = filePath.replace(svnCutHead+"resources/","");
                            sourceFile = new File(targetPath+"/WEB-INF/classes/"+filePath);
                            targetFile = new File(copyPath+"/WEB-INF/classes/"+filePath);
                        }

                        if (filePath.endsWith("pom.xml")){
                            continue;
                        }

                        System.out.println("原始文件："+sourceFile.getPath());
                        System.out.println("复制文件："+targetFile.getPath());

                        if (!sourceFile.exists()){
                            throw new RuntimeException("原始文件无法找到!");
                        }
                        if (!targetFile.getParentFile().exists()){
                            targetFile.getParentFile().mkdirs();
                        }
                        FileChannel in = null;
                        FileChannel out = null;
                        FileInputStream inputStream = null;
                        FileOutputStream outputStream = null;
                        try {
                            inputStream = new FileInputStream(sourceFile);
                            outputStream = new FileOutputStream(targetFile);
                            in = inputStream.getChannel();
                            out = outputStream.getChannel();
                            in.transferTo(0,in.size(),out);
                        }catch (IOException e){
                            throw new RuntimeException("文件复制错误！");
                        }finally {
                            inputStream.close();
                            in.close();
                            outputStream.close();
                            out.close();
                        }


                    }
                }
            }
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
}
