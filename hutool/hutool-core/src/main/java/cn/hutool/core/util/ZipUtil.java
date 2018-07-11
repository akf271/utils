package cn.hutool.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;

/**
 * 压缩工具类
 * 
 * @author Looly
 *
 */
public class ZipUtil {
	
	/** 默认编码，使用平台相关编码 */
	private static final Charset DEFAULT_CHARSET = CharsetUtil.defaultCharset();
	
	/**
	 * 打包到当前目录，使用默认编码UTF-8
	 * 
	 * @param srcPath 源文件路径
	 * @return 打包好的压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(String srcPath) throws UtilException {
		return zip(srcPath, DEFAULT_CHARSET);
	}

	/**
	 * 打包到当前目录
	 * 
	 * @param srcPath 源文件路径
	 * @param charset 编码
	 * @return 打包好的压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(String srcPath, Charset charset) throws UtilException {
		return zip(FileUtil.file(srcPath), charset);
	}
	
	/**
	 * 打包到当前目录，使用默认编码UTF-8
	 * 
	 * @param srcFile 源文件或目录
	 * @return 打包好的压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(File srcFile) throws UtilException {
		return zip(srcFile, DEFAULT_CHARSET);
	}

	/**
	 * 打包到当前目录
	 * 
	 * @param srcFile 源文件或目录
	 * @param charset 编码
	 * @return 打包好的压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(File srcFile, Charset charset) throws UtilException {
		File zipFile = FileUtil.file(srcFile.getParentFile(), FileUtil.mainName(srcFile) + ".zip");
		zip(zipFile, charset, false, srcFile);
		return zipFile;
	}

	/**
	 * 对文件或文件目录进行压缩<br>
	 * 不包含被打包目录
	 * 
	 * @param srcPath 要压缩的源文件路径。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
	 * @param zipPath 压缩文件保存的路径，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @return 压缩好的Zip文件
	 * @throws UtilException IO异常
	 */
	public static File zip(String srcPath, String zipPath) throws UtilException {
		return zip(srcPath, zipPath, false);
	}
	
	/**
	 * 对文件或文件目录进行压缩<br>
	 * 
	 * @param srcPath 要压缩的源文件路径。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
	 * @param zipPath 压缩文件保存的路径，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param withSrcDir 是否包含被打包目录
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(String srcPath, String zipPath, boolean withSrcDir) throws UtilException {
		return zip(srcPath, zipPath, DEFAULT_CHARSET, withSrcDir);
	}

	/**
	 * 对文件或文件目录进行压缩<br>
	 * 
	 * @param srcPath 要压缩的源文件路径。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
	 * @param zipPath 压缩文件保存的路径，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param charset 编码
	 * @param withSrcDir 是否包含被打包目录
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(String srcPath, String zipPath, Charset charset, boolean withSrcDir) throws UtilException {
		File srcFile = FileUtil.file(srcPath);
		File zipFile = FileUtil.file(zipPath);
		zip(zipFile, charset, withSrcDir, srcFile);
		return zipFile;
	}
	
	/**
	 * 对文件或文件目录进行压缩<br>
	 * 使用默认UTF-8编码
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param withSrcDir 是否包含被打包目录
	 * @param srcFiles 要压缩的源文件或目录。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(File zipFile, boolean withSrcDir, File... srcFiles) throws UtilException {
		return zip(zipFile, DEFAULT_CHARSET, withSrcDir, srcFiles);
	}

	/**
	 * 对文件或文件目录进行压缩
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param charset 编码
	 * @param withSrcDir 是否包含被打包目录
	 * @param srcFiles 要压缩的源文件或目录。如果压缩一个文件，则为该文件的全路径；如果压缩一个目录，则为该目录的顶层目录路径
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 */
	public static File zip(File zipFile, Charset charset, boolean withSrcDir, File... srcFiles) throws UtilException {
		validateFiles(zipFile, srcFiles);

		try (ZipOutputStream out = getZipOutputStream(zipFile, charset)){
			String srcRootDir;
			for (File srcFile : srcFiles) {
				// 如果只是压缩一个文件，则需要截取该文件的父目录
				srcRootDir = srcFile.getCanonicalPath();
				if (srcFile.isFile() || withSrcDir) {
					srcRootDir = srcFile.getParent();
				}
				// 调用递归压缩方法进行目录或文件压缩
				zip(srcFile, srcRootDir, out);
				out.flush();
			}
		} catch (IOException e) {
			throw new UtilException(e);
		}
		return zipFile;
	}
	
	/**
	 * 对流中的数据加入到压缩文件，使用默认UTF-8编码
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param path 流数据在压缩文件中的路径或文件名
	 * @param data 要压缩的数据
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 * @since 3.0.6
	 */
	public static File zip(File zipFile, String path, String data) throws UtilException {
		return zip(zipFile, path, data, DEFAULT_CHARSET);
	}

	/**
	 * 对流中的数据加入到压缩文件<br>
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param path 流数据在压缩文件中的路径或文件名
	 * @param data 要压缩的数据
	 * @param charset 编码
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 * @since 3.2.2
	 */
	public static File zip(File zipFile, String path, String data, Charset charset) throws UtilException {
		return zip(zipFile, path, IoUtil.toStream(data, charset), charset);
	}
	
	/**
	 * 对流中的数据加入到压缩文件<br>
	 * 使用默认编码UTF-8
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param path 流数据在压缩文件中的路径或文件名
	 * @param in 要压缩的源
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 * @since 3.0.6
	 */
	public static File zip(File zipFile, String path, InputStream in) throws UtilException {
		return zip(zipFile, path, in, DEFAULT_CHARSET);
	}
	
	/**
	 * 对流中的数据加入到压缩文件<br>
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param path 流数据在压缩文件中的路径或文件名
	 * @param in 要压缩的源
	 * @param charset 编码
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 * @since 3.2.2
	 */
	public static File zip(File zipFile, String path, InputStream in, Charset charset) throws UtilException {
		return zip(zipFile, new String[] {path}, new InputStream[] {in}, charset);
	}
	
	/**
	 * 对流中的数据加入到压缩文件<br>
	 * 路径列表和流列表长度必须一致
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param paths 流数据在压缩文件中的路径或文件名
	 * @param ins 要压缩的源
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 * @since 3.0.9
	 */
	public static File zip(File zipFile, String[] paths, InputStream[] ins) throws UtilException {
		return zip(zipFile, paths, ins, DEFAULT_CHARSET);
	}

	/**
	 * 对流中的数据加入到压缩文件<br>
	 * 路径列表和流列表长度必须一致
	 * 
	 * @param zipFile 生成的Zip文件，包括文件名。注意：zipPath不能是srcPath路径下的子文件夹
	 * @param paths 流数据在压缩文件中的路径或文件名
	 * @param ins 要压缩的源
	 * @param charset 编码
	 * @return 压缩文件
	 * @throws UtilException IO异常
	 * @since 3.0.9
	 */
	public static File zip(File zipFile, String[] paths, InputStream[] ins, Charset charset) throws UtilException {
		if(ArrayUtil.isEmpty(paths) || ArrayUtil.isEmpty(ins)) {
			throw new IllegalArgumentException("Paths or ins is empty !");
		}
		if(paths.length != ins.length) {
			throw new IllegalArgumentException("Paths length is not equals to ins length !");
		}
		
		ZipOutputStream out = null;
		try {
			out = getZipOutputStream(zipFile, charset);
			for(int i = 0; i < paths.length; i++) {
				addFile(ins[i], paths[i], out);
			}
		} finally {
			IoUtil.close(out);
		}
		return zipFile;
	}
	
	//---------------------------------------------------------------------------------------------- Unzip
	/**
	 * 解压到文件名相同的目录中，默认编码UTF-8
	 * 
	 * @param zipFilePath 压缩文件路径
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 */
	public static File unzip(String zipFilePath) throws UtilException {
		return unzip(zipFilePath, DEFAULT_CHARSET);
	}
	
	/**
	 * 解压到文件名相同的目录中
	 * 
	 * @param zipFilePath 压缩文件路径
	 * @param charset 编码
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 * @since 3.2.2
	 */
	public static File unzip(String zipFilePath, Charset charset) throws UtilException {
		return unzip(FileUtil.file(zipFilePath), charset);
	}
	
	/**
	 * 解压到文件名相同的目录中，使用UTF-8编码
	 * 
	 * @param zipFile 压缩文件
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 * @since 3.2.2
	 */
	public static File unzip(File zipFile) throws UtilException {
		return unzip(zipFile, DEFAULT_CHARSET);
	}

	/**
	 * 解压到文件名相同的目录中
	 * 
	 * @param zipFile 压缩文件
	 * @param charset 编码
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 * @since 3.2.2
	 */
	public static File unzip(File zipFile, Charset charset) throws UtilException {
		return unzip(zipFile, FileUtil.file(zipFile.getParentFile(), FileUtil.mainName(zipFile)), charset);
	}

	/**
	 * 解压，默认UTF-8编码
	 * 
	 * @param zipFilePath 压缩文件的路径
	 * @param outFileDir 解压到的目录
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 */
	public static File unzip(String zipFilePath, String outFileDir) throws UtilException {
		return unzip(zipFilePath, outFileDir, DEFAULT_CHARSET);
	}
	
	/**
	 * 解压
	 * 
	 * @param zipFilePath 压缩文件的路径
	 * @param outFileDir 解压到的目录
	 * @param charset 编码
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 */
	public static File unzip(String zipFilePath, String outFileDir, Charset charset) throws UtilException {
		return unzip(FileUtil.file(zipFilePath), FileUtil.mkdir(outFileDir), charset);
	}
	
	/**
	 * 解压，默认使用UTF-8编码
	 * 
	 * @param zipFile zip文件
	 * @param outFile 解压到的目录
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 */
	public static File unzip(File zipFile, File outFile) throws UtilException {
		return unzip(zipFile, outFile, DEFAULT_CHARSET);
	}

	/**
	 * 解压
	 * 
	 * @param zipFile zip文件
	 * @param outFile 解压到的目录
	 * @param charset 编码
	 * @return 解压的目录
	 * @throws UtilException IO异常
	 * @since 3.2.2
	 */
	@SuppressWarnings("unchecked")
	public static File unzip(File zipFile, File outFile, Charset charset) throws UtilException {
		charset = (null == charset) ? DEFAULT_CHARSET : charset;
		
		ZipFile zipFileObj = null;
		try {
			zipFileObj = new ZipFile(zipFile, charset);
			final Enumeration<ZipEntry> em = (Enumeration<ZipEntry>) zipFileObj.entries();
			ZipEntry zipEntry = null;
			File outItemFile = null;
			while (em.hasMoreElements()) {
				zipEntry = em.nextElement();
				outItemFile = new File(outFile, zipEntry.getName());
				if (zipEntry.isDirectory()) {
					outItemFile.mkdirs();
				} else {
					FileUtil.touch(outItemFile);
					copy(zipFileObj, zipEntry, outItemFile);
				}
			}
		} catch (IOException e) {
			throw new UtilException(e);
		} finally {
			IoUtil.close(zipFileObj);

		}
		return outFile;
	}

	// ----------------------------------------------------------------------------- Gzip
	/**
	 * Gzip压缩处理
	 * 
	 * @param content 被压缩的字符串
	 * @param charset 编码
	 * @return 压缩后的字节流
	 * @throws UtilException IO异常
	 */
	public static byte[] gzip(String content, String charset) throws UtilException {
		return gzip(StrUtil.bytes(content, charset));
	}

	/**
	 * Gzip压缩处理
	 * 
	 * @param val 被压缩的字节流
	 * @return 压缩后的字节流
	 * @throws UtilException IO异常
	 */
	public static byte[] gzip(byte[] val) throws UtilException {
		FastByteArrayOutputStream bos = new FastByteArrayOutputStream(val.length);
		GZIPOutputStream gos = null;
		try {
			gos = new GZIPOutputStream(bos);
			gos.write(val, 0, val.length);
			gos.finish();
			gos.flush();
			val = bos.toByteArray();
		} catch (IOException e) {
			throw new UtilException(e);
		} finally {
			IoUtil.close(gos);
		}
		return val;
	}

	/**
	 * Gzip压缩文件
	 * 
	 * @param file 被压缩的文件
	 * @return 压缩后的字节流
	 * @throws UtilException IO异常
	 */
	public static byte[] gzip(File file) throws UtilException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
		GZIPOutputStream gos = null;
		BufferedInputStream in;
		try {
			gos = new GZIPOutputStream(bos);
			in = FileUtil.getInputStream(file);
			IoUtil.copy(in, gos);
			return bos.toByteArray();
		} catch (IOException e) {
			throw new UtilException(e);
		} finally {
			IoUtil.close(gos);
		}
	}

	/**
	 * Gzip解压缩处理
	 * 
	 * @param buf 压缩过的字节流
	 * @param charset 编码
	 * @return 解压后的字符串
	 * @throws UtilException IO异常
	 */
	public static String unGzip(byte[] buf, String charset) throws UtilException {
		return StrUtil.str(unGzip(buf), charset);
	}

	/**
	 * Gzip解压处理
	 * 
	 * @param buf buf
	 * @return bytes
	 * @throws UtilException IO异常
	 */
	public static byte[] unGzip(byte[] buf) throws UtilException {
		GZIPInputStream gzi = null;
		ByteArrayOutputStream bos = null;
		try {
			gzi = new GZIPInputStream(new ByteArrayInputStream(buf));
			bos = new ByteArrayOutputStream(buf.length);
			IoUtil.copy(gzi, bos);
			buf = bos.toByteArray();
		} catch (IOException e) {
			throw new UtilException(e);
		} finally {
			IoUtil.close(gzi);
		}
		return buf;
	}

	// ---------------------------------------------------------------------------------------------- Private method start
	/**
	 * 获得 {@link ZipOutputStream}
	 * 
	 * @param zipFile 压缩文件
	 * @param charset 编码
	 * @return {@link ZipOutputStream}
	 */
	private static ZipOutputStream getZipOutputStream(File zipFile, Charset charset) {
		return getZipOutputStream(FileUtil.getOutputStream(zipFile), charset);
	}
	
	/**
	 * 获得 {@link ZipOutputStream}
	 * 
	 * @param zipFile 压缩文件
	 * @param charset 编码
	 * @return {@link ZipOutputStream}
	 */
	private static ZipOutputStream getZipOutputStream(OutputStream out, Charset charset) {
		charset = (null == charset) ? DEFAULT_CHARSET : charset;
		return new ZipOutputStream(out, charset);
	}

	/**
	 * 递归压缩文件夹
	 * 
	 * @param out 压缩文件存储对象
	 * @param srcRootDir 压缩文件夹根目录的子路径
	 * @param file 当前递归压缩的文件或目录对象
	 * @throws UtilException IO异常
	 */
	private static void zip(File file, String srcRootDir, ZipOutputStream out) throws UtilException {
		if (file == null) {
			return;
		}
		
		final String subPath = FileUtil.subPath(srcRootDir, file); // 获取文件相对于压缩文件夹根目录的子路径
		if(file.isDirectory()){// 如果是目录，则压缩压缩目录中的文件或子目录
			final File[] files = file.listFiles();
			if(ArrayUtil.isEmpty(files) && StrUtil.isNotEmpty(subPath)) {
				//加入目录，只有空目录时才加入目录，非空时会在创建文件时自动添加父级目录
				addDir(subPath, out);
			}
			//压缩目录下的子文件或目录
			for (File childFile : files) {
				zip(childFile, srcRootDir, out);
			}
		} else {// 如果是文件或其它符号，则直接压缩该文件
			addFile(file, subPath, out);
		}
	}
	
	/**
	 * 添加文件到压缩包
	 * 
	 * @param file 需要压缩的文件
	 * @param path 在压缩文件中的路径
	 * @param out 压缩文件存储对象
	 * @throws UtilException IO异常
	 * @since 4.0.5
	 */
	private static void addFile(File file, String path, ZipOutputStream out) throws UtilException {
		BufferedInputStream in = null;
		try {
			in = FileUtil.getInputStream(file);
			addFile(in, path, out);
		} finally {
			IoUtil.close(in);
		}
	}

	/**
	 * 添加文件流到压缩包，不关闭输入流
	 * 
	 * @param in 需要压缩的输入流
	 * @param path 压缩的路径
	 * @param out 压缩文件存储对象
	 * @throws UtilException IO异常
	 */
	private static void addFile(InputStream in, String path, ZipOutputStream out) throws UtilException {
		if(null == in) {
			return;
		}
		try {
			out.putNextEntry(new ZipEntry(path));
			IoUtil.copy(in, out);
		} catch (IOException e) {
			throw new UtilException(e);
		} finally {
			closeEntry(out);
		}
	}
	
	/**
	 * 在压缩包中新建目录
	 * 
	 * @param path 压缩的路径
	 * @param out 压缩文件存储对象
	 * @throws UtilException IO异常
	 */
	private static void addDir(String path, ZipOutputStream out) throws UtilException {
		path = StrUtil.addSuffixIfNot(path, StrUtil.SLASH);
		try {
			out.putNextEntry(new ZipEntry(path));
		} catch (IOException e) {
			throw new UtilException(e);
		} finally {
			closeEntry(out);
		}
	}

	/**
	 * 判断压缩文件保存的路径是否为源文件路径的子文件夹，如果是，则抛出异常（防止无限递归压缩的发生）
	 * 
	 * @param zipFile 压缩后的产生的文件路径
	 * @param srcFile 被压缩的文件或目录
	 */
	private static void validateFiles(File zipFile, File... srcFiles) throws UtilException {
		for (File srcFile : srcFiles) {
			if (false == srcFile.exists()) {
				throw new UtilException(StrUtil.format("File [{}] not exist!", srcFile.getAbsolutePath()));
			}

			try {
				// 压缩文件不能位于被压缩的目录内
				if (srcFile.isDirectory() && zipFile.getParent().contains(srcFile.getCanonicalPath())) {
					throw new UtilException("[zipPath] must not be the child directory of [srcPath]!");
				}

				if (false == zipFile.exists()) {
					FileUtil.touch(zipFile);
				}
			} catch (IOException e) {
				throw new UtilException(e);
			}
		}
	}

	/**
	 * 关闭当前Entry，继续下一个Entry
	 * 
	 * @param out ZipOutputStream
	 */
	private static void closeEntry(ZipOutputStream out) {
		try {
			out.closeEntry();
		} catch (IOException e) {
			//ignore
		}
	}

	/**
	 * 从Zip文件流中拷贝文件出来
	 * 
	 * @param zipFile Zip文件
	 * @param zipEntry zip文件中的子文件
	 * @param outItemFile 输出到的文件
	 * @throws IOException IO异常
	 */
	private static void copy(ZipFile zipFile, ZipEntry zipEntry, File outItemFile) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = zipFile.getInputStream(zipEntry);
			out = FileUtil.getOutputStream(outItemFile);
			IoUtil.copy(in, out);
		} finally {
			IoUtil.close(out);
			IoUtil.close(in);
		}
	}
	// ---------------------------------------------------------------------------------------------- Private method end

}