package org.examle.xbrl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import oracle.xdb.XMLType;

public final class RunQueries {

	private static final class Options {

		private final String dbUrl;
		private final String user;
		private final String pwd;

		public Options(final String dbUrl, final String user, final String pwd) {
			this.dbUrl = dbUrl;
			this.user = user;
			this.pwd = pwd;

		}
	}

	private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:orcl";
	private static final String DB_USER = "xbrlrep";
	private static final String DB_PWD = "xbrlrep";

	/**
	 * Execute the SQL statements in Oracle XBRL's USGAAP 2008 example from
	 * JDBC.
	 * 
	 * @param args
	 *            optional array of up to three strings overriding database url,
	 *            database user and password, respectively.
	 * @throws SQLException
	 *             if there is an error with the database or driver.
	 */
	public static void main(final String[] args) throws SQLException {
		final Options opts = parseArgs(args);
		try (final Connection connection = DriverManager.getConnection(
				opts.dbUrl, opts.user, opts.pwd);
				final Statement stmt = connection.createStatement()) {
			queryUsGaapTaxonomy(connection, stmt);
			validateAndLoadNewReportSubmissions(stmt);
			queryInstanceAndGenerateViews(stmt);

		}
	}

	private static Options parseArgs(final String[] args) {
		switch (args.length) {
		case 3:
			return new Options(args[0], args[1], args[2]);
		case 2:
			return new Options(args[0], args[1], DB_PWD);
		case 1:
			return new Options(args[0], DB_USER, DB_PWD);
		case 0:
			return new Options(DB_URL, DB_USER, DB_PWD);

		default:
			final String pn = RunQueries.class.getSimpleName();
			System.err.printf("%s: Usage:%n"
					+ "\t%s [database-url] [db-user] [db-password]%n", pn, pn);
			System.exit(-1);
			break;
		}
		return null;
	}

	private static void queryInstanceAndGenerateViews(final Statement stmt)
			throws SQLException {
		executeQuery(
				stmt,
				"SELECT * FROM (SELECT instance_path, item_id, arc_arcrole, "
						+ "                      footnote_role, footnote_title, "
						+ "                      footnote_lang, footnote_content "
						+ "               FROM oraxbrl_footnotes "
						+ "               ORDER BY instance_path, item_id) "
						+ "WHERE ROWNUM < 10");
		executeQuery(stmt, "SELECT DBMS_ORAXBRLI.Instance_Network("
				+ "  'http://xbrl.boa.com/2006-12-31', " + "  '0000070858', "
				+ "  DATE'2005-01-01', " + "  DATE'2006-01-01', "
				+ "  'http://xbrl.boa.com/2006-12-31/ext/IncomeStatement', "
				+ "  NULL, " + "  'presentationArc', "
				+ "  'http://www.xbrl.org/2003/arcrole/parent-child', "
				+ "  NULL, " + "  NULL, " + "  NULL, " + "  'en-US', "
				+ "  1) " + "FROM DUAL");
		// If the proc. fails with a user-defined exception, drop the
		// view 'boa' if it exists, e.g:
		// $ echo "drop view xbrlrep.boa;" | sqlplus xbrlrep
		execute(stmt, "DROP view xbrlrep.boa");
		execute(stmt, "CALL DBMS_ORAXBRLV.createHyperCubeSuperFactTable("
				+ "  'boa', " + "  '0000070858', "
				+ "  'http://xbrl.boa.com/2006-12-31', "
				+ "  'http://xbrl.us/us-gaap/2008-03-31', "
				+ "  'StatementTable', "
				+ "  'http://xbrl.boa.com/2006-12-31/ext/StockholdersEquity', "
				+ "  'segment', "
				+ "  'http://xbrl.boa.com/2006-12-31/ext/StockholdersEquity')");
	}

	private static void validateAndLoadNewReportSubmissions(final Statement stmt)
			throws SQLException {
		execute(stmt, "CALL DBMS_ORAXBRL.loadSchema('/boa/boa-20061231.xsd', "
				+ "XMLType(BFILENAME('USGAAP', '/boa/boa-20061231.xsd'), "
				+ "nls_charset_id('AL32UTF8')))");
		execute(stmt,
				"CALL DBMS_ORAXBRL.loadLinkbase('/boa/boa-20061231_pre.xml', "
						+ "   XMLType(BFILENAME('USGAAP', "
						+ "             '/boa/boa-20061231_pre.xml'), "
						+ "   nls_charset_id('AL32UTF8')))");
		execute(stmt,
				"CALL DBMS_ORAXBRL.loadLinkbase('/boa/boa-20061231_def.xml', "
						+ "XMLType(BFILENAME('USGAAP', '/boa/boa-20061231_def.xml'), "
						+ "nls_charset_id('AL32UTF8')))");
		execute(stmt,
				"CALL DBMS_ORAXBRL.loadLinkbase('/boa/boa-20061231_lab.xml', "
						+ "XMLType(BFILENAME('USGAAP', '/boa/boa-20061231_lab.xml'), "
						+ "nls_charset_id('AL32UTF8')))");
		execute(stmt,
				"CALL DBMS_ORAXBRL.loadLinkbase('/boa/boa-20061231_cal.xml', "
						+ "XMLType(BFILENAME('USGAAP', '/boa/boa-20061231_cal.xml'), "
						+ "nls_charset_id('AL32UTF8')))");
		execute(stmt,
				"CALL DBMS_ORAXBRL.loadInstance('/boa/boa-20061231_XML.xml', "
						+ "XMLType(BFILENAME('USGAAP', '/boa/boa-20061231_XML.xml'), "
						+ "nls_charset_id('AL32UTF8')))");
	}

	private static void queryUsGaapTaxonomy(final Connection connection,
			final Statement stmt) throws SQLException {
		executeQuery(stmt, "SELECT count(*) FROM oraxbrl_xs_element");
		executeQuery(stmt, "SELECT count(*) FROM oraxbrl_calculation_linkbase");
		executeQuery(stmt, "SELECT count(*) FROM oraxbrl_pres_linkbase");
		final String qry = "SELECT DBMS_ORAXBRLT.concepts_network("
				+ "'http://xbrl.us/us-gaap-entryPoint-std/2008-03-31', "
				+ "'http://xbrl.us/us-gaap/role/statement/StatementOfIncome', "
				+ "NULL, 'presentationArc', "
				+ "'http://www.xbrl.org/2003/arcrole/parent-child', "
				+ "'http://www.xbrl.org/2003/role/link', "
				+ "'http://www.xbrl.org/2003/arcrole/concept-label', "
				+ "'http://www.xbrl.org/2003/role/label','en-US', NULL) "
				+ "FROM DUAL";
		executeQuery(stmt, qry);
		callProcedure(connection, "DBMS_ORAXBRLV.createViewForConceptTree",
				"pres_network",
				"http://xbrl.us/us-gaap-entryPoint-std/2008-03-31", null, null,
				"http://xbrl.us/us-gaap/role/statement/StatementOfIncome",
				null, "presentationArc",
				"http://www.xbrl.org/2003/arcrole/parent-child", null, null,
				null, "en-US", Integer.valueOf(-1));

	}

	private static void callProcedure(final Connection connection,
			final String fnName, final Object... args) throws SQLException {
		final StringBuilder sb = new StringBuilder();
		if (args.length > 0) {
			sb.append("?");
		}
		for (int i = 1; i < args.length; i++) {
			sb.append(", ?");
		}
		final String callExpr = String.format("{call %s(%s)}", fnName,
				sb.toString());
		try (CallableStatement stmt = connection.prepareCall(callExpr)) {
			int i = 1;
			for (final Object arg : args) {
				stmt.setObject(i, arg);
				i++;
			}
			System.out.println(String.format("%s with parameters: %s => ",
					callExpr, Arrays.asList(args)));
			stmt.execute();
			System.out.println(" executed");

		}
	}

	private static void execute(final Statement stmt, final String qry)
			throws SQLException {
		final boolean result = stmt.execute(qry);
		assert !result;
		System.out.println(String.format("%s => %s updates", qry,
				stmt.getUpdateCount()));
	}

	private static void executeQuery(final Statement stmt, final String qry)
			throws SQLException {
		final ResultSet rs = stmt.executeQuery(qry);
		System.out.println(String.format("%s => ", qry));
		final int cc = rs.getMetaData().getColumnCount();
		for (int i = 1; i <= cc; i++) {
			System.out.print(rs.getMetaData().getColumnLabel(i) + "\t");
		}
		System.out.println();
		while (rs.next()) {
			for (int i = 1; i <= cc; i++) {
				final Object obj = rs.getObject(i);
				System.out.print(obj + "\t");
				if (obj instanceof XMLType) {
					System.out.printf("%nn%s%n", ((XMLType) obj).getString());
				}
			}
			System.out.println();
		}
		assert !rs.next();
	}
}
