/**
 * 
 * :-::-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-+:-+:-+:-+:-++:-:+:-:+:-:+:-:
 * 
 * This file is part of CHiLOⓇ  - http://www.cccties.org/en/activities/chilo/
 *   CHiLOⓇ is a next-generation learning system utilizing ebooks,  aiming 
 *   at dissemination of open education.
 *                          Copyright 2015 NPO CCC-TIES
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * :-::-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-:+:-+:-+:-+:-+:-++:-:+:-:+:-:+:-:
 * 
 */
 package epub3maker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

/**
 * ユーティリティクラス．
 *
 * static 化できる汎用メソッドはこちらに移す．
 * @author tueda
 *
 */
public class Util {

    /**
     * Debug 用 println<br>
     * log level:<br>
     * LOG_EMERG       0       system is unusable<br>
     * LOG_ALERT       1       action must be taken immediately<br>
     * LOG_CRIT        2       critical conditions<br>
     * LOG_ERR         3       error conditions<br>
     * LOG_WARNING     4       /* warning conditions<br>
     * LOG_NOTICE      5       /* normal but significant condition<br>
     * LOG_INFO        6       /* informational<br>
     * LOG_DEBUG       7       /* debug-level messages<br>
     *
     * @param level log level.
     * @param str log文字列
     */
    public static void infoPrintln(LogLevel level, String str) {
        Calendar cal = Calendar.getInstance();

        int year = cal.get(Calendar.YEAR);        //(2)現在の年を取得
        int month = cal.get(Calendar.MONTH) + 1;  //(3)現在の月を取得
        int day = cal.get(Calendar.DATE);         //(4)現在の日を取得
        int hour = cal.get(Calendar.HOUR_OF_DAY); //(5)現在の時を取得
        int minute = cal.get(Calendar.MINUTE);    //(6)現在の分を取得
        int second = cal.get(Calendar.SECOND);    //(7)現在の秒を取得

        if (level.compareTo(LogLevel.valueOf(Config.getCurrentLogLevel())) <= 0) {
            System.err.printf("[%4d/%02d/%02d %02d:%02d:%02d] %s (%s)\n",
                    year, month, day, hour, minute, second, str, level);
        }
    }

    /**
     * コピー元のファイルから、コピー先のファイルへ ファイルのコピーを行います。
     *
     * @param src
     *            コピー元のパス
     * @param dest
     *            コピー先のパス
     * @throws IOException
     *             IOException
     */
    public static void copyTransfer(final Path src, final Path dest)
            throws IOException {

        if (Files.isDirectory(src)) {
            // ディレクトリがない場合、作成
            if(!Files.exists(dest)){
                Files.createDirectories(dest);
            }

            List<Path> list = new ArrayList<Path>();
            Stream<Path> stream = Files.list(src);
            stream.forEach(list::add);

            for (Path file : list) {
                Path srcFile = src.resolve(file.getFileName());
                Path destFile = dest.resolve(file.getFileName());

                copyTransfer(srcFile, destFile);
            }
            stream.close();
        } else {
            // ファイルのコピー
            Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean isValueValid(String value)
    {
        return value != null && !value.isEmpty();
    }

    public static void cleanUpDirectory(Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            try (Stream<Path> stream = Files.walk(targetPath)) {
                List<Path> filePaths = new ArrayList<>();

                // rootDirectory以下にあるディレクトリ以外のファイルをすべてitemsにaddしていく。
                stream.filter(entry -> !(Files.isDirectory(entry))).forEach(
                        filePaths::add);
                for (Path file : filePaths) {
                    Files.delete(file);
                }
            }
        }
    }

    public static void initializeDirectory(Path targetDirectory) throws IOException {
        /*
         * directory があるかチェック
         */
        if (Files.exists(targetDirectory)) {
            Util.cleanUpDirectory(targetDirectory);
        } else {
            Files.createDirectories(targetDirectory);
        }
    }

    public static List<Path> getFilePaths(Path rootDirectory,
    	    Predicate<? super Path> predicate) throws IOException {
        List<Path> filePaths = new ArrayList<>();
        if(!Files.exists(rootDirectory))
        {
            return filePaths;
        }
        try (Stream<Path> stream = Files.walk(rootDirectory)) {
            stream.filter(predicate).forEach(filePaths::add);
        }
        return filePaths;
    }

    // rootDirectory以下にあるディレクトリ以外のファイルをすべてListにaddしていく。
    public static List<Path> getFilePaths(Path rootDirectory) throws IOException {
        return getFilePaths(rootDirectory, entry -> !(Files.isDirectory(entry)));
    }

    public static List<Path> findFilesIn(Path targetDirectory,
            BiPredicate<Path, BasicFileAttributes> matcher) throws IOException {

        List<Path> files = new ArrayList<Path>();
        if (Files.exists(targetDirectory)) {
            try (Stream<Path> stream = Files.find(targetDirectory, 1, matcher)) {
                stream.forEach(files::add);
            }
        } else {
            Util.infoPrintln(LogLevel.LOG_DEBUG, "findFilesIn: ### NOTICE ### : Not exists : "
                    + targetDirectory);
        }
        return files;
    }
    
    /**
     * ディレクトリ内でprefixから始まるファイルのリストを作成する
     * @param targetDirectory
     * @param prefix
     * @return
     * @throws IOException
     */
    public static List<Path> findFilesPrefix(Path targetDirectory,
            String prefix) throws IOException {
        return findFilesIn(targetDirectory, (file, attrs) -> file.getFileName()
                .toString().startsWith(prefix)
                && !attrs.isDirectory());
    }

    /**
     * ディレクトリ内でextesionで終わるファイルのリストを作成する
     * @param targetDirectory
     * @param extension
     * @return
     * @throws IOException
     */
    public static List<Path> findFilesIn(Path targetDirectory,
            String extension) throws IOException {
        return findFilesIn(targetDirectory, (file, attrs) -> file.getFileName()
                .toString().endsWith(extension)
                && !attrs.isDirectory());
    }

    public static List<Path> findCssFilesIn(Path targetDirectory) throws IOException {
        return findFilesIn(targetDirectory, ".css");
    }

    public static List<Path> findStaticsFilesIn(Path targetDirectory)
            throws IOException {
        return findFilesIn(targetDirectory, ".xhtml");
    }

    public static List<Path> findImagesFilesIn(Path targetDirectory)
            throws IOException {
        return findFilesIn(targetDirectory, (file, attrs) -> !file
                .getFileName().toString().startsWith(".")
                && !attrs.isDirectory());
    }

    public static List<Path> findVideosFilesIn(Path targetDirectory)
            throws IOException {
        return findFilesIn(targetDirectory, ".mp4");
    }

    public static List<Path> findScriptFilesIn(Path targetDirectory) throws IOException {
        return findFilesIn(targetDirectory, ".js");
    }
    
    public static boolean isPublishHtml() {
    	if (Config.getPublishStyle().equalsIgnoreCase("html")) {
    		return true;
    	} else {
    		return false;
    	}
    }

    public static String pageFileName(int vol, int page)
    {
    	if (isPublishHtml()) {
    		return String.format("vol-%03d-%03d.html", vol, page);	
    	} else {
    		return String.format("vol-%03d-%03d.xhtml", vol, page);	
    	}
    	
    }
    
    public static String getModifiedTime()
    {
        DateTimeFormatter dtf = DateTimeFormatter
        		.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return LocalDateTime.now(Clock.systemUTC()).format(dtf);
    }
    
    public static String getModifiedTime2()
    {
        DateTimeFormatter dtf = DateTimeFormatter
        		.ofPattern("yyyy-MM-dd");
        return LocalDateTime.now(Clock.systemUTC()).format(dtf);
    }
    
    public static int IMAGE_SIZE_4_3 = 1;
    public static int IMAGE_SIZE_16_9 = 2;
    
    public static int imageFileSizeType(Path filePath)
    {
    	int ret = IMAGE_SIZE_16_9;
    	if (Files.exists(filePath)) {
    		BufferedImage image = null;
    		try {
    			image = ImageIO.read(new File(filePath.toString()));
    			int height = image.getHeight();
    			int width = image.getWidth();
    			int diff1 = Math.abs(300 - ((height * 4 * 100) / width));
    			int diff2 = Math.abs(900 - ((height * 16 * 100)) / width);
    			if (diff1 < diff2) {
    				ret = IMAGE_SIZE_4_3;
    			}
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    	return ret;
    }

    public static String getRelativePath(Path ps1, Path ps2)
    {
    	if (ps1 == null || ps2 == null)
    		return null;
    	
    	return ps1.relativize(ps2).toString().replaceAll("\\\\", "/");
    }

    public static String getRelativePath(PageSetting ps1, PageSetting ps2)
    {
    	if (ps1 == null || ps2 == null)
    		return null;

    	return Util.getRelativePath(ps1.getTextPathForArchiveFile().getParent(), ps2.getTextPathForArchiveFile());
    }
    
    public static String volumeImagePath(int volume, String fileName) {
    	if (!Util.isValueValid(fileName))
    		return null;
    	else 
    		return "../../" + Volume.KEY_VOLUME_PREFIX + volume + "/images/" + fileName;
    }
    
    public static String commonImagePath(String fileName) {
        if (!Util.isValueValid(fileName))
            return null;
        else
            return "../../common/images/" + fileName;
    }

    public static String authorImagePath(String fileName) {
        if (!Util.isValueValid(fileName))
            return null;
        else
            return "../../common/authorImages/" + fileName;
    }
    
    public static int stringLengthRelative(String s) {
        double len = 0.0;

        double zenRatio = Config.getSVGTextZenkakuRatio();
        double hanRatio = Config.getSVGTextHankakuRatio();
        
        int hancount = 0;
        int zencount = 0;
        for( int i=0; i<s.length(); i++ ) {
            char c = s.charAt( i );
            if( ( c<='\u007e' )|| // 英数字
                ( c=='\u00a5' )|| // \記号
                ( c=='\u203e' )|| // ~記号
                ( c>='\uff61' && c<='\uff9f' ) // 半角カナ
            ) {
                hancount++;
            }
            else {
                zencount++;
            }
        }
        
        infoPrintln(LogLevel.LOG_DEBUG, "stringLengthRelative: [" + s + "] zen=" + zencount + ", han=" + hancount);
        
        len = zencount * zenRatio + hancount * hanRatio;
        
        return (int)Math.ceil(len);
    }
}
