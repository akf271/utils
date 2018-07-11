package cn.hutool.db.sql;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import cn.hutool.db.DbUtil;
import cn.hutool.db.StatementUtil;
import cn.hutool.db.handler.RsHandler;

/**
 * SQL执行器，全部为静态方法，执行查询或非查询的SQL语句<br>
 * 此方法为JDBC的简单封装，与数据库类型无关
 * @author loolly
 *
 */
public class SqlExecutor {
	
	/**
	 * 执行非查询语句<br>
	 * 语句包括 插入、更新、删除<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param conn 数据库连接对象
	 * @param sql SQL，使用name做为占位符，例如:name
	 * @param paramMap 参数Map
	 * @return 影响的行数
	 * @throws SQLException SQL执行异常
	 * @since 4.0.10
	 */
	public static int execute(Connection conn, String sql, Map<String, Object> paramMap) throws SQLException {
		final NamedSql namedSql = new NamedSql(sql, paramMap);
		return execute(conn, namedSql.getSql(), namedSql.getParams());
	}
	
	/**
	 * 执行非查询语句<br>
	 * 语句包括 插入、更新、删除<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param conn 数据库连接对象
	 * @param sql SQL
	 * @param params 参数
	 * @return 影响的行数
	 * @throws SQLException SQL执行异常
	 */
	public static int execute(Connection conn, String sql, Object... params) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return executeUpdate(ps, params);
		} finally {
			DbUtil.close(ps);
		}
	}
	
	/**
	 * 执行调用存储过程<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param conn 数据库连接对象
	 * @param sql SQL
	 * @param params 参数
	 * @return 是否成功
	 * @throws SQLException SQL执行异常
	 */
	public static boolean call(Connection conn, String sql, Object... params) throws SQLException {
		CallableStatement ps = null;
		try {
			ps = conn.prepareCall(sql);
			return execute(ps, params);
		} finally {
			DbUtil.close(ps);
		}
	}
	
	/**
	 * 执行非查询语句，返回主键<br>
	 * 发查询语句包括 插入、更新、删除<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param conn 数据库连接对象
	 * @param sql SQL
	 * @param paramMap 参数Map
	 * @return 主键
	 * @throws SQLException SQL执行异常
	 * @since 4.0.10
	 */
	public static Long executeForGeneratedKey(Connection conn, String sql, Map<String, Object> paramMap) throws SQLException {
		final NamedSql namedSql = new NamedSql(sql, paramMap);
		return executeForGeneratedKey(conn, namedSql.getSql(), namedSql.getParams());
	}
	
	/**
	 * 执行非查询语句，返回主键<br>
	 * 发查询语句包括 插入、更新、删除<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param conn 数据库连接对象
	 * @param sql SQL
	 * @param params 参数
	 * @return 主键
	 * @throws SQLException SQL执行异常
	 */
	public static Long executeForGeneratedKey(Connection conn, String sql, Object... params) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			executeUpdate(ps, params);
			ResultSet rs = ps.getGeneratedKeys(); 
			if(rs != null && rs.next()) {
				try {
					return rs.getLong(1);
				} catch (SQLException e) {
					//可能会出现没有主键返回的情况
				}
			}
			return null;
		} finally {
			DbUtil.close(ps);
		}
	}
	
	/**
	 * 批量执行非查询语句<br>
	 * 语句包括 插入、更新、删除<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param conn 数据库连接对象
	 * @param sql SQL
	 * @param paramsBatch 批量的参数
	 * @return 每个SQL执行影响的行数
	 * @throws SQLException SQL执行异常
	 */
	public static int[] executeBatch(Connection conn, String sql, Object[]... paramsBatch) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			for (Object[] params : paramsBatch) {
				StatementUtil.fillParams(ps, params);
				ps.addBatch();
			}
			return ps.executeBatch();
		} finally {
			DbUtil.close(ps);
		}
	}
	
	/**
	 * 执行查询语句<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param <T> 处理结果类型
	 * @param conn 数据库连接对象
	 * @param sql 查询语句，使用参数名占位符，例如:name
	 * @param rsh 结果集处理对象
	 * @param paramMap 参数对
	 * @return 结果对象
	 * @throws SQLException SQL执行异常
	 * @since 4.0.10
	 */
	public static <T> T query(Connection conn, String sql, RsHandler<T> rsh, Map<String, Object> paramMap) throws SQLException {
		final NamedSql namedSql = new NamedSql(sql, paramMap);
		return query(conn, namedSql.getSql(), rsh, namedSql.getParams());
	}
	
	/**
	 * 执行查询语句<br>
	 * 此方法不会关闭Connection
	 * 
	 * @param <T> 处理结果类型
	 * @param conn 数据库连接对象
	 * @param sql 查询语句
	 * @param rsh 结果集处理对象
	 * @param params 参数
	 * @return 结果对象
	 * @throws SQLException SQL执行异常
	 */
	public static <T> T query(Connection conn, String sql, RsHandler<T> rsh, Object... params) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			return query(ps, rsh, params);
		} finally {
			DbUtil.close(ps);
		}
	}
	
	//-------------------------------------------------------------------------------------- Execute With PreparedStatement
	/**
	 * 执行非查询语句<br>
	 * 语句包括 插入、更新、删除<br>
	 * 此方法不会关闭PreparedStatement
	 * 
	 * @param ps PreparedStatement对象
	 * @param params 参数
	 * @return 影响的行数
	 * @throws SQLException SQL执行异常
	 */
	public static int executeUpdate(PreparedStatement ps, Object... params) throws SQLException {
		StatementUtil.fillParams(ps, params);
		return ps.executeUpdate();
	}
	
	/**
	 * 执行非查询语句<br>
	 * 语句包括 插入、更新、删除<br>
	 * 此方法不会关闭PreparedStatement
	 * 
	 * @param ps PreparedStatement对象
	 * @param params 参数
	 * @return 影响的行数
	 * @throws SQLException SQL执行异常
	 */
	public static boolean execute(PreparedStatement ps, Object... params) throws SQLException {
		StatementUtil.fillParams(ps, params);
		return ps.execute();
	}
	
	/**
	 * 执行查询语句<br>
	 * 此方法不会关闭PreparedStatement
	 * 
	 * @param <T> 处理结果类型
	 * @param ps PreparedStatement
	 * @param rsh 结果集处理对象
	 * @param params 参数
	 * @return 结果对象
	 * @throws SQLException SQL执行异常
	 */
	public static <T> T query(PreparedStatement ps, RsHandler<T> rsh, Object... params) throws SQLException {
		ResultSet rs = null;
		try {
			StatementUtil.fillParams(ps, params);
			rs = ps.executeQuery();
			return rsh.handle(rs);
		} finally {
			DbUtil.close(rs);
		}
	}
	
	/**
	 * 执行查询语句并关闭PreparedStatement
	 * 
	 * @param <T> 处理结果类型
	 * @param ps PreparedStatement
	 * @param rsh 结果集处理对象
	 * @param params 参数
	 * @return 结果对象
	 * @throws SQLException SQL执行异常
	 */
	public static <T> T queryAndClosePs(PreparedStatement ps, RsHandler<T> rsh, Object... params) throws SQLException {
		try {
			return query(ps, rsh, params);
		} finally{
			DbUtil.close(ps);
		}
	}
}
