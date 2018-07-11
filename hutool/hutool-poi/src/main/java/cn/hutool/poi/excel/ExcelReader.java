package cn.hutool.poi.excel;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.cell.CellEditor;
import cn.hutool.poi.excel.cell.CellUtil;
import cn.hutool.poi.excel.editors.TrimEditor;

/**
 * Excel读取器<br>
 * 读取Excel工作簿
 * 
 * @author Looly
 * @since 3.1.0
 */
public class ExcelReader implements Closeable {

	/** 是否被关闭 */
	private boolean isClosed;
	/** 工作簿 */
	private Workbook workbook;
	/** Excel中对应的Sheet */
	private Sheet sheet;

	/** 是否忽略空行 */
	private boolean ignoreEmptyRow = true;
	/** 单元格值处理接口 */
	private CellEditor cellEditor;
	/** 标题别名 */
	private Map<String, String> headerAlias = new HashMap<>();

	// ------------------------------------------------------------------------------------------------------- Constructor start
	/**
	 * 构造
	 * 
	 * @param excelFilePath Excel文件路径，绝对路径或相对于ClassPath路径
	 * @param sheetIndex sheet序号，0表示第一个sheet
	 */
	public ExcelReader(String excelFilePath, int sheetIndex) {
		this(FileUtil.file(excelFilePath), sheetIndex);
	}

	/**
	 * 构造
	 * 
	 * @param bookFile Excel文件
	 * @param sheetIndex sheet序号，0表示第一个sheet
	 */
	public ExcelReader(File bookFile, int sheetIndex) {
		this(WorkbookUtil.createBook(bookFile), sheetIndex);
	}

	/**
	 * 构造
	 * 
	 * @param bookFile Excel文件
	 * @param sheetName sheet名，第一个默认是sheet1
	 */
	public ExcelReader(File bookFile, String sheetName) {
		this(WorkbookUtil.createBook(bookFile), sheetName);
	}

	/**
	 * 构造
	 * 
	 * @param bookStream Excel文件的流
	 * @param sheetIndex sheet序号，0表示第一个sheet
	 * @param closeAfterRead 读取结束是否关闭流
	 */
	public ExcelReader(InputStream bookStream, int sheetIndex, boolean closeAfterRead) {
		this(WorkbookUtil.createBook(bookStream, closeAfterRead), sheetIndex);
	}

	/**
	 * 构造
	 * 
	 * @param bookStream Excel文件的流
	 * @param sheetName sheet名，第一个默认是sheet1
	 * @param closeAfterRead 读取结束是否关闭流
	 */
	public ExcelReader(InputStream bookStream, String sheetName, boolean closeAfterRead) {
		this(WorkbookUtil.createBook(bookStream, closeAfterRead), sheetName);
	}

	/**
	 * 构造
	 * 
	 * @param book {@link Workbook} 表示一个Excel文件
	 * @param sheetIndex sheet序号，0表示第一个sheet
	 */
	public ExcelReader(Workbook book, int sheetIndex) {
		this(book.getSheetAt(sheetIndex));
	}

	/**
	 * 构造
	 * 
	 * @param book {@link Workbook} 表示一个Excel文件
	 * @param sheetName sheet名，第一个默认是sheet1
	 */
	public ExcelReader(Workbook book, String sheetName) {
		this(book.getSheet(sheetName));
	}

	/**
	 * 构造
	 * 
	 * @param sheet Excel中的sheet
	 */
	public ExcelReader(Sheet sheet) {
		Assert.notNull(sheet, "No Sheet provided.");
		this.sheet = sheet;
		this.workbook = sheet.getWorkbook();
	}
	// ------------------------------------------------------------------------------------------------------- Constructor end

	// ------------------------------------------------------------------------------------------------------- Getters and Setters start
	/**
	 * 获取读取的Workbook
	 * 
	 * @return Workbook
	 * @since 4.0.0
	 */
	public Workbook getWorkbook() {
		return this.workbook;
	}

	/**
	 * 返回工作簿表格数
	 * 
	 * @return 工作簿表格数
	 * @since 4.0.10
	 */
	public int getSheetCount() {
		return this.workbook.getNumberOfSheets();
	}

	/**
	 * 获取此工作簿所有Sheet表
	 * 
	 * @return sheet表列表
	 * @since 4.0.3
	 */
	public List<Sheet> getSheets() {
		final int totalSheet = getSheetCount();
		final List<Sheet> result = new ArrayList<>(totalSheet);
		for (int i = 0; i < totalSheet; i++) {
			result.add(this.workbook.getSheetAt(i));
		}
		return result;
	}

	/**
	 * 获取表名列表
	 * 
	 * @return 表名列表
	 * @since 4.0.3
	 */
	public List<String> getSheetNames() {
		final int totalSheet = workbook.getNumberOfSheets();
		List<String> result = new ArrayList<>(totalSheet);
		for (int i = 0; i < totalSheet; i++) {
			result.add(this.workbook.getSheetAt(i).getSheetName());
		}
		return result;
	}

	/**
	 * 获取当前编辑的sheet
	 * 
	 * @return sheet
	 * @since 4.0.0
	 */
	public Sheet getSheet() {
		return this.sheet;
	}

	/**
	 * 自定义需要读取的Sheet
	 * 
	 * @param sheetName sheet名
	 * @return this
	 * @since 4.0.10
	 */
	public ExcelReader setSheet(String sheetName) {
		this.sheet = this.workbook.getSheet(sheetName);
		return this;
	}

	/**
	 * 自定义需要读取的Sheet
	 * 
	 * @param sheetIndex sheet序号，从0开始计数
	 * @return this
	 * @since 4.0.10
	 */
	public ExcelReader setSheet(int sheetIndex) {
		this.sheet = this.workbook.getSheetAt(sheetIndex);
		return this;
	}

	/**
	 * 是否忽略空行
	 * 
	 * @return 是否忽略空行
	 */
	public boolean isIgnoreEmptyRow() {
		return ignoreEmptyRow;
	}

	/**
	 * 设置是否忽略空行
	 * 
	 * @param ignoreEmptyRow 是否忽略空行
	 * @return this
	 */
	public ExcelReader setIgnoreEmptyRow(boolean ignoreEmptyRow) {
		this.ignoreEmptyRow = ignoreEmptyRow;
		return this;
	}

	/**
	 * 设置单元格值处理逻辑<br>
	 * 当Excel中的值并不能满足我们的读取要求时，通过传入一个编辑接口，可以对单元格值自定义，例如对数字和日期类型值转换为字符串等
	 * 
	 * @param cellEditor 单元格值处理接口
	 * @return this
	 * @see TrimEditor
	 */
	public ExcelReader setCellEditor(CellEditor cellEditor) {
		this.cellEditor = cellEditor;
		return this;
	}

	/**
	 * 获得标题行的别名Map
	 * 
	 * @return 别名Map
	 */
	public Map<String, String> getHeaderAlias() {
		return headerAlias;
	}

	/**
	 * 设置标题行的别名Map
	 * 
	 * @param headerAlias 别名Map
	 * @return this
	 */
	public ExcelReader setHeaderAlias(Map<String, String> headerAlias) {
		this.headerAlias = headerAlias;
		return this;
	}

	/**
	 * 增加标题别名
	 * 
	 * @param header 标题
	 * @param alias 别名
	 * @return this
	 */
	public ExcelReader addHeaderAlias(String header, String alias) {
		this.headerAlias.put(header, alias);
		return this;
	}

	/**
	 * 去除标题别名
	 * 
	 * @param header 标题
	 * @return this
	 */
	public ExcelReader removeHeaderAlias(String header) {
		this.headerAlias.remove(header);
		return this;
	}
	// ------------------------------------------------------------------------------------------------------- Getters and Setters end

	/**
	 * 读取工作簿中指定的Sheet的所有行列数据
	 * 
	 * @return 行的集合，一行使用List表示
	 */
	public List<List<Object>> read() {
		return read(0);
	}

	/**
	 * 读取工作簿中指定的Sheet
	 * 
	 * @param startRowIndex 起始行（包含，从0开始计数）
	 * @return 行的集合，一行使用List表示
	 * @since 4.0.0
	 */
	public List<List<Object>> read(int startRowIndex) {
		return read(startRowIndex, Integer.MAX_VALUE);
	}

	/**
	 * 读取工作簿中指定的Sheet
	 * 
	 * @param startRowIndex 起始行（包含，从0开始计数）
	 * @param endRowIndex 结束行（包含，从0开始计数）
	 * @return 行的集合，一行使用List表示
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<List<Object>> read(int startRowIndex, int endRowIndex) {
		checkNotClosed();
		List<List<Object>> resultList = new ArrayList<>();

		startRowIndex = Math.max(startRowIndex, sheet.getFirstRowNum());// 读取起始行（包含）
		endRowIndex = Math.min(endRowIndex, sheet.getLastRowNum());// 读取结束行（包含）
		boolean isFirstLine = true;
		List rowList;
		for (int i = startRowIndex; i <= endRowIndex; i++) {
			rowList = readRow(i);
			if (CollUtil.isNotEmpty(rowList) || false == ignoreEmptyRow) {
				if (null == rowList) {
					rowList = new ArrayList<>(0);
				}
				if (isFirstLine) {
					isFirstLine = false;
					if (MapUtil.isNotEmpty(headerAlias)) {
						rowList = aliasHeader(rowList);
					}
				}
				resultList.add(rowList);
			}
		}
		return resultList;
	}

	/**
	 * 读取Excel为Map的列表，读取所有行，默认第一行做为标题，数据从第二行开始<br>
	 * Map表示一行，标题为key，单元格内容为value
	 * 
	 * @return Map的列表
	 */
	public List<Map<String, Object>> readAll() {
		return read(0, 1, Integer.MAX_VALUE);
	}

	/**
	 * 读取Excel为Map的列表<br>
	 * Map表示一行，标题为key，单元格内容为value
	 * 
	 * @param headerRowIndex 标题所在行，如果标题行在读取的内容行中间，这行做为数据将忽略
	 * @param startRowIndex 起始行（包含，从0开始计数）
	 * @param endRowIndex 读取结束行（包含，从0开始计数）
	 * @return Map的列表
	 */
	public List<Map<String, Object>> read(int headerRowIndex, int startRowIndex, int endRowIndex) {
		checkNotClosed();
		// 边界判断
		final int firstRowNum = sheet.getFirstRowNum();
		final int lastRowNum = sheet.getLastRowNum();
		if (headerRowIndex < firstRowNum) {
			throw new IndexOutOfBoundsException(StrUtil.format("Header row index {} is lower than first row index {}.", headerRowIndex, firstRowNum));
		} else if (headerRowIndex > lastRowNum) {
			throw new IndexOutOfBoundsException(StrUtil.format("Header row index {} is greater than last row index {}.", headerRowIndex, firstRowNum));
		}
		startRowIndex = Math.max(startRowIndex, firstRowNum);// 读取起始行（包含）
		endRowIndex = Math.min(endRowIndex, lastRowNum);// 读取结束行（包含）

		// 读取header
		List<Object> headerList = readRow(sheet.getRow(headerRowIndex));

		final List<Map<String, Object>> result = new ArrayList<>(endRowIndex - startRowIndex + 1);
		List<Object> rowList;
		for (int i = startRowIndex; i <= endRowIndex; i++) {
			if (i != headerRowIndex) {
				// 跳过标题行
				rowList = readRow(sheet.getRow(i));
				if (CollUtil.isNotEmpty(rowList) || false == ignoreEmptyRow) {
					if (null == rowList) {
						rowList = new ArrayList<>(0);
					}
					result.add(IterUtil.toMap(aliasHeader(headerList), rowList));
				}
			}
		}
		return result;
	}

	/**
	 * 读取Excel为Bean的列表，读取所有行，默认第一行做为标题，数据从第二行开始
	 * 
	 * @param <T> Bean类型
	 * @param beanType 每行对应Bean的类型
	 * @return Map的列表
	 */
	public <T> List<T> readAll(Class<T> beanType) {
		return read(0, 1, Integer.MAX_VALUE, beanType);
	}

	/**
	 * 读取Excel为Bean的列表
	 * 
	 * @param <T> Bean类型
	 * @param headerRowIndex 标题所在行，如果标题行在读取的内容行中间，这行做为数据将忽略，，从0开始计数
	 * @param startRowIndex 起始行（包含，从0开始计数）
	 * @param beanType 每行对应Bean的类型
	 * @return Map的列表
	 * @since 4.0.1
	 */
	public <T> List<T> read(int headerRowIndex, int startRowIndex, Class<T> beanType) {
		return read(headerRowIndex, startRowIndex, Integer.MAX_VALUE, beanType);
	}

	/**
	 * 读取Excel为Bean的列表
	 * 
	 * @param <T> Bean类型
	 * @param headerRowIndex 标题所在行，如果标题行在读取的内容行中间，这行做为数据将忽略，，从0开始计数
	 * @param startRowIndex 起始行（包含，从0开始计数）
	 * @param endRowIndex 读取结束行（包含，从0开始计数）
	 * @param beanType 每行对应Bean的类型
	 * @return Map的列表
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> read(int headerRowIndex, int startRowIndex, int endRowIndex, Class<T> beanType) {
		checkNotClosed();
		final List<Map<String, Object>> mapList = read(headerRowIndex, startRowIndex, endRowIndex);
		if (Map.class.isAssignableFrom(beanType)) {
			return (List<T>) mapList;
		}

		final List<T> beanList = new ArrayList<>(mapList.size());
		for (Map<String, Object> map : mapList) {
			beanList.add(BeanUtil.mapToBean(map, beanType, false));
		}
		return beanList;
	}

	/**
	 * 读取为文本格式<br>
	 * 使用{@link ExcelExtractor} 提取Excel内容
	 * 
	 * @param withSheetName 是否附带sheet名
	 * @return Excel文本
	 * @since 4.1.0
	 */
	public String readAsText(boolean withSheetName) {
		final ExcelExtractor extractor = getExtractor();
		extractor.setIncludeSheetNames(withSheetName);
		return extractor.getText();
	}

	/**
	 * 获取 {@link ExcelExtractor} 对象
	 * 
	 * @return {@link ExcelExtractor}
	 * @since 4.1.0
	 */
	public ExcelExtractor getExtractor() {
		ExcelExtractor extractor;
		Workbook wb = this.workbook;
		if (wb instanceof HSSFWorkbook) {
			extractor = new org.apache.poi.hssf.extractor.ExcelExtractor((HSSFWorkbook) wb);
		} else {
			extractor = new XSSFExcelExtractor((XSSFWorkbook) wb);
		}
		return extractor;
	}

	/**
	 * 读取某一行数据
	 * 
	 * @param rowIndex 行号，从0开始
	 * @return 一行数据
	 * @since 4.0.3
	 */
	public List<Object> readRow(int rowIndex) {
		return readRow(this.sheet.getRow(rowIndex));
	}

	/**
	 * 读取某个单元格的值
	 * 
	 * @param x X坐标，从0计数，既列号
	 * @param y Y坐标，从0计数，既行号
	 * @return 值，如果单元格无值返回null
	 * @since 4.0.3
	 */
	public Object readCellValue(int x, int y) {
		return CellUtil.getCellValue(getCell(x, y), this.cellEditor);
	}

	/**
	 * 获取指定坐标单元格
	 * 
	 * @param x X坐标，从0计数，既列号
	 * @param y Y坐标，从0计数，既行号
	 * @return {@link Cell}
	 * @since 4.0.5
	 */
	public Cell getCell(int x, int y) {
		return getCell(x, y, false);
	}

	/**
	 * 获取或创建指定坐标单元格
	 * 
	 * @param x X坐标，从0计数，既列号
	 * @param y Y坐标，从0计数，既行号
	 * @return {@link Cell}
	 * @since 4.0.6
	 */
	public Cell getOrCreateCell(int x, int y) {
		return getCell(x, y, true);
	}

	/**
	 * 获取指定坐标单元格
	 * 
	 * @param x X坐标，从0计数，既列号
	 * @param y Y坐标，从0计数，既行号
	 * @param isCreateIfNotExist 单元格不存在时是否创建
	 * @return {@link Cell}
	 * @since 4.0.6
	 */
	public Cell getCell(int x, int y, boolean isCreateIfNotExist) {
		final Row row = isCreateIfNotExist ? RowUtil.getOrCreateRow(this.sheet, y) : this.sheet.getRow(y);
		if (null != row) {
			return isCreateIfNotExist ? CellUtil.getOrCreateCell(row, x) : row.getCell(x);
		}
		return null;
	}

	/**
	 * 关闭工作簿
	 * 
	 * @since 3.2.0
	 */
	@Override
	public void close() {
		IoUtil.close(this.workbook);
		this.sheet = null;
		this.workbook = null;
		this.isClosed = true;
	}

	/**
	 * 获取Excel写出器<br>
	 * 在读取Excel并做一定编辑后，获取写出器写出
	 * 
	 * @return {@link ExcelWriter}
	 * @since 4.0.6
	 */
	public ExcelWriter getWriter() {
		return new ExcelWriter(this.sheet);
	}

	// ------------------------------------------------------------------------------------------------------- Private methods start
	/**
	 * 读取一行
	 * 
	 * @param row 行
	 * @return 单元格值列表
	 */
	private List<Object> readRow(Row row) {
		return RowUtil.readRow(row, this.cellEditor);
	}

	/**
	 * 转换标题别名，如果没有别名则使用原标题
	 * 
	 * @param headerList 原标题列表
	 * @return 转换别名列表
	 */
	private List<String> aliasHeader(List<Object> headerList) {
		final ArrayList<String> result = new ArrayList<>();
		if (CollUtil.isEmpty(headerList)) {
			return result;
		}

		String header;
		String alias = null;
		for (Object headerObj : headerList) {
			if (null != headerObj) {
				header = headerObj.toString();
				alias = this.headerAlias.get(header);
				if (null == alias) {
					// 无别名则使用原标题
					alias = header;
				}
			}
			result.add(alias);
		}
		return result;
	}

	/**
	 * 检查是否未关闭状态
	 */
	private void checkNotClosed() {
		Assert.isFalse(this.isClosed, "ExcelReader has been closed!");
	}
	// ------------------------------------------------------------------------------------------------------- Private methods end
}
