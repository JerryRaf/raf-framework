package com.raf.framework.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * https://www.jianshu.com/p/501bc9d4f90a
 */
@Slf4j
public class FortifyUtil {

    /**
     * 私有构造器,防止实例化工具类
     */
    private FortifyUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String DOT_STR = ".";

    /**
     * 校验文件名是否正常，不校验后缀
     *
     * @param fileName 文件名全称
     * @return true 正常  false 异常
     */
    public static boolean checkFileName(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }

        // 判断文件名中是否包含/ \ .
        if (fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }

        if (fileName.substring(0, fileName.lastIndexOf(DOT_STR)).contains(".")) {
            return false;
        }

        return true;
    }

    /**
     * 校验文件名全称是否正常
     * 1.后缀是否符号预期，2.不能包含/ \符号以及去除.扩展名后不能包含.符号
     *
     * @param fileName      文件名
     * @param fileTypeEnums 文件格式
     * @return true 正常  false 异常
     */
    public static boolean checkFileName(String fileName, FileTypeEnum... fileTypeEnums) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }

        if (null == fileTypeEnums || fileTypeEnums.length == 0) {
            return checkFileName(fileName);
        }

        boolean flag = false;
        int fileEnd = 0;
        for (FileTypeEnum fileTypeEnum : fileTypeEnums) {
            if (fileName.toUpperCase().endsWith(fileTypeEnum.getKey())) {
                flag = true;
                // fileEnd 包含点号，例如 ".JPG" 长度为 key长度+1
                fileEnd = fileTypeEnum.getKey().length() + 1;
                break;
            }
        }

        // 文件类型不匹配
        if (!flag) {
            return false;
        }

        // 判断文件名中是否包含/ \
        // 以及在去除文件后缀 + . 的情况下是否包含 .
        if (fileName.contains("/") || fileName.contains("\\") || fileName.substring(0, fileName.length() - fileEnd)
                .contains(".")) {
            return false;
        }

        return true;
    }

    private static final Map<String, String> FILE_TYPE_MAP = new HashMap<String, String>();

    static {
        // 加载文件类型
        getAllFileType();
    }

    /**
     * 验证上传文件的文件类型
     *
     * @param file 上传的文件
     * @return
     */
    public static String getFileHeader(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase(Locale.ENGLISH);

        if ("txt".equalsIgnoreCase(fileType) || "ppt".equalsIgnoreCase(fileType) || "tiff".equalsIgnoreCase(fileType) || "cebx".equalsIgnoreCase(fileType) || "3gp".equalsIgnoreCase(fileType) || "mpeg".equalsIgnoreCase(fileType) || "swf".equalsIgnoreCase(fileType) || "ceb".equalsIgnoreCase(fileType)) {
            return fileType;
        }

        String value = null;

        try (InputStream is = file.getInputStream()) {
            byte[] b = new byte[4];
            is.read(b, 0, b.length);
            value = bytesToHexString(b);
        } catch (IOException e) {
            return null;
        }

        String result = null;
        Iterator<String> keyIter = FILE_TYPE_MAP.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();

            if (!StringUtils.isEmpty(value)) {
                if (key.toLowerCase(Locale.ENGLISH)
                        .startsWith(value.toLowerCase(Locale.ENGLISH)) || value.toLowerCase(Locale.ENGLISH)
                        .startsWith(key.toLowerCase(Locale.ENGLISH))) {

                    result = FILE_TYPE_MAP.get(key);
                    break;
                }
            }
        }

        return result == null ? fileType : result;
    }

    /**
     * 得到上传文件的文件头
     *
     * @param src 文件头字节数组
     * @return
     */
    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (null == src || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    private static void getAllFileType() {
        FILE_TYPE_MAP.put("ffd8ffe000104a464946", "jpg");
        FILE_TYPE_MAP.put("89504e470d0a1a0a0000", "png");
        FILE_TYPE_MAP.put("47494638396126026f01", "gif");
        FILE_TYPE_MAP.put("49492a00227105008037", "tif");
        FILE_TYPE_MAP.put("424d228c010000000000", "bmp"); // 16色位图(bmp)
        FILE_TYPE_MAP.put("424d8240090000000000", "bmp"); // 24位位图(bmp)
        FILE_TYPE_MAP.put("424d8e1b030000000000", "bmp"); // 256色位图(bmp)
        FILE_TYPE_MAP.put("41433130313500000000", "dwg");
        FILE_TYPE_MAP.put("3c21444f435459504520", "html");
        FILE_TYPE_MAP.put("3c21646f637479706520", "htm");
        FILE_TYPE_MAP.put("48544d4c207b0d0a0942", "css");
        FILE_TYPE_MAP.put("696b2e71623d696b2e71", "js");
        FILE_TYPE_MAP.put("7b5c727466315c616e73", "rtf");
        FILE_TYPE_MAP.put("38425053000100000000", "psd");
        FILE_TYPE_MAP.put("46726f6d3a203d3f6762", "eml");
        FILE_TYPE_MAP.put("d0cf11e0a1b11ae10000", "doc"); // MS Excel、Word、Msi
        FILE_TYPE_MAP.put("d0cf11e0a1b11ae10000", "vsd");
        FILE_TYPE_MAP.put("5374616E64617264204A", "mdb");
        FILE_TYPE_MAP.put("255044462d312e350d0a", "pdf");
        FILE_TYPE_MAP.put("2e524d46000000120001", "rmvb"); // rmvb、rm
        FILE_TYPE_MAP.put("464c5601050000000900", "flv"); // flv、f4v
        FILE_TYPE_MAP.put("00000020667479706d70", "mp4");
        FILE_TYPE_MAP.put("49443303000000002176", "mp3");
        FILE_TYPE_MAP.put("000001ba210001000180", "mpg");
        FILE_TYPE_MAP.put("3026b2758e66cf11a6d9", "wmv"); // wmv、asf
        FILE_TYPE_MAP.put("52494646e27807005741", "wav");
        FILE_TYPE_MAP.put("52494646d07d60074156", "avi");
        FILE_TYPE_MAP.put("4d546864000000060001", "mid");
        FILE_TYPE_MAP.put("504b0304140000000800", "zip");
        FILE_TYPE_MAP.put("526172211a0700cf9073", "rar");
        FILE_TYPE_MAP.put("235468697320636f6e66", "ini");
        FILE_TYPE_MAP.put("504b03040a0000000000", "jar");
        FILE_TYPE_MAP.put("4d5a9000030000000400", "exe");
        FILE_TYPE_MAP.put("3c25402070616765206c", "jsp");
        FILE_TYPE_MAP.put("4d616e69666573742d56", "mf");
        FILE_TYPE_MAP.put("3c3f786d6c2076657273", "xml");
        FILE_TYPE_MAP.put("494e5345525420494e54", "sql");
        FILE_TYPE_MAP.put("7061636b616765207765", "java");
        FILE_TYPE_MAP.put("406563686f206f66660d", "bat");
        FILE_TYPE_MAP.put("1f8b0800000000000000", "gz");
        FILE_TYPE_MAP.put("6c6f67346a2e726f6f74", "properties");
        FILE_TYPE_MAP.put("cafebabe0000002e0041", "class");
        FILE_TYPE_MAP.put("49545346030000006000", "chm");
        FILE_TYPE_MAP.put("04000000010000001300", "mxp");
        FILE_TYPE_MAP.put("504b0304140006000800", "docx");
        FILE_TYPE_MAP.put("d0cf11e0a1b11ae10000", "wps");// WPS(wps、et、dps)
        FILE_TYPE_MAP.put("6431303a637265617465", "torrent");
        FILE_TYPE_MAP.put("6D6F6F76", "mov");
        FILE_TYPE_MAP.put("FF575043", "wpd");
        FILE_TYPE_MAP.put("CFAD12FEC5FD746F", "dbx");
        FILE_TYPE_MAP.put("2142444E", "pst");
        FILE_TYPE_MAP.put("AC9EBD8F", "qdf");
        FILE_TYPE_MAP.put("E3828596", "pwl");
        FILE_TYPE_MAP.put("2E7261FD", "ram");
    }

    public static boolean check(MultipartFile file, FileTypeEnum... fileTypeEnums) {
        if (!checkFileName(file.getOriginalFilename(), fileTypeEnums)) {
            return false;
        }

        String headType = getFileHeader(file);
        for (FileTypeEnum fileTypeEnum : fileTypeEnums) {
            if (fileTypeEnum.getKey().equalsIgnoreCase(headType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * .数量=1, /数量必须<=10，不能有window的// \\符号
     *
     * @param filePath
     * @return
     */
    public static boolean checkPath(String filePath) {
        long dotCount = filePath.chars().filter(c -> c == '.').count();
        long slashCount = filePath.chars().filter(c -> c == '/').count();
        if (dotCount != 1) {
            return false;
        }

        if (slashCount > 10) {
            return false;
        }

        if (filePath.contains("//") || filePath.contains("\\")) {
            return false;
        }
        return true;
    }
}