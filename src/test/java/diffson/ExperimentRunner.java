package diffson;

import java.io.File;
import java.util.Date;

import org.junit.Test;

import fr.inria.astor.core.setup.ConfigurationProperties;

/**
 * Experiment runners
 */
public class ExperimentRunner {

	@Test
	public void testICSE2015() throws Exception {
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer();
		analyzer.run(ConfigurationProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void testICSE15() throws Exception {
		ConfigurationProperties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "20");
		File outFile = new File("./out/ICSE2015_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("./datasets/icse2015").getAbsolutePath();
		ConfigurationProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ConfigurationProperties.getProperty("icse15difffolder"));

	}

	@Test
	public void testD4J() throws Exception {
		ConfigurationProperties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "200");
		File outFile = new File("./out/Defects4J_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("./datasets/Defects4J").getAbsolutePath();
		ConfigurationProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ConfigurationProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void testCODEREP() throws Exception {
		ConfigurationProperties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("max_synthesis_step", "100000");
		diffson.ConfigurationProperties.properties.setProperty("excludetests", "false");
		for (int i = 3; i <= 4; i++) {
			File outFile = new File("./out/" + "codeRepDS" + i + "_" + (new Date()));
			String out = outFile.getAbsolutePath();
			outFile.mkdirs();
			DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
			String input = new File(
					// "./datasets/codeRepDS" + i
					"/Users/matias/develop/sketch-repair/git-sketch4repair/datasets/CodeRep/ds_pairs/result_Dataset" + i
							+ "_unidiff").getAbsolutePath();
			ConfigurationProperties.properties.setProperty("icse15difffolder", input);
			analyzer.run(ConfigurationProperties.getProperty("icse15difffolder"));
		}
	}

	@Test
	public void testD4Reload() throws Exception {
		ConfigurationProperties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "200");
		File outFile = new File("./out/Defects4JReload_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("/Users/matias/develop/defects4-repair-reloaded/pairs/D_unassessed/").getAbsolutePath();
		ConfigurationProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ConfigurationProperties.getProperty("icse15difffolder"));
	}

	@Test
	public void test3Sfix() throws Exception {
		ConfigurationProperties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("max_synthesis_step", "100000");
		ConfigurationProperties.properties.setProperty("MAX_AST_CHANGES_PER_FILE", "200");
		File outFile = new File("./out/3fixtest_" + (new Date()));
		String out = outFile.getAbsolutePath();
		outFile.mkdirs();
		DiffContextAnalyzer analyzer = new DiffContextAnalyzer(out);
		String input = new File("/Users/matias/develop/overfitting/overfitting-data/data/rowdata/3sFix_files_pair/")
				.getAbsolutePath();
		ConfigurationProperties.properties.setProperty("icse15difffolder", input);
		analyzer.run(ConfigurationProperties.getProperty("icse15difffolder"));
	}

}