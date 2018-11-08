package add.features.detector.repairpatterns;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gumtreediff.tree.ITree;

import add.entities.PatternInstance;
import add.entities.RepairPatterns;
import add.features.detector.spoon.RepairPatternUtils;
import add.features.detector.spoon.SpoonHelper;
import add.features.detector.spoon.filter.NullCheckFilter;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtIf;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.visitor.filter.LineFilter;

/**
 * Created by fermadeiral
 */
public class MissingNullCheckDetector extends AbstractPatternDetector {
	private static final String MISS_NULL_CHECK_N = "missNullCheckN";
	private static final String MISS_NULL_CHECK_P = "missNullCheckP";
	private static Logger LOGGER = LoggerFactory.getLogger(MissingNullCheckDetector.class);

	public MissingNullCheckDetector(List<Operation> operations) {
		super(operations);
	}

	@Override
	public void detect(RepairPatterns repairPatterns) {
		for (Operation operation : this.operations) {
			if (operation instanceof InsertOperation) {
				CtElement srcNode = operation.getSrcNode();
				SpoonHelper.printInsertOrDeleteOperation(srcNode.getFactory().getEnvironment(), srcNode, operation);

				List<CtBinaryOperator> binaryOperatorList = srcNode.getElements(new NullCheckFilter());
				for (CtBinaryOperator binaryOperator : binaryOperatorList) {
					if (RepairPatternUtils.isNewBinaryOperator(binaryOperator)) {
						if (RepairPatternUtils.isNewConditionInBinaryOperator(binaryOperator)) {
							LOGGER.debug("-New null check: " + binaryOperator.toString());

							final CtElement referenceExpression;
							if (binaryOperator.getRightHandOperand().toString().equals("null")) {
								referenceExpression = binaryOperator.getLeftHandOperand();
							} else {
								referenceExpression = binaryOperator.getRightHandOperand();
							}
							LOGGER.debug("-Reference expression: " + referenceExpression.toString());

							boolean wasPatternFound = false;

							List soldt = null;
							List soldelse = null;

							CtVariable variable = RepairPatternUtils
									.getVariableFromReferenceExpression(referenceExpression);
							if (variable == null || (variable != null && !RepairPatternUtils.isNewVariable(variable))) {
								wasPatternFound = true;
								//
								if (binaryOperator.getParent() instanceof CtConditional) {
									CtConditional c = (CtConditional) binaryOperator.getParent();
									CtElement thenExpr = c.getThenExpression();
									CtElement elseExp = c.getElseExpression();

									if (thenExpr != null) {
										soldt = new ArrayList<>();
										if (thenExpr.getMetadata("new") == null)
											soldt.add(thenExpr);

									}
									if (elseExp != null) {
										soldelse = new ArrayList<>();
										if (elseExp.getMetadata("new") == null)
											soldelse.add(elseExp);
									}

								}

							} // else {
							CtElement parent = binaryOperator.getParent(new LineFilter());

							if (parent instanceof CtIf) {
								CtBlock thenBlock = ((CtIf) parent).getThenStatement();
								CtBlock elseBlock = ((CtIf) parent).getElseStatement();

								if (thenBlock != null) {
									soldt = RepairPatternUtils
											.getIsThereOldStatementInStatementList(thenBlock.getStatements());
									if (!soldt.isEmpty())
										wasPatternFound = true;

								} else if (elseBlock != null) {
									soldelse = RepairPatternUtils
											.getIsThereOldStatementInStatementList(elseBlock.getStatements());
									if (!soldelse.isEmpty())
										wasPatternFound = true;
								}
							}

							// }
							if (wasPatternFound) {

								List susp = new ArrayList<>();
								if (soldt != null)
									susp.addAll(soldt);
								if (soldelse != null)
									susp.addAll(soldelse);

								CtElement lineP = null;
								ITree lineTree = null;
								if (!susp.isEmpty()) {

									lineP = MappingAnalysis.getParentLine(new LineFilter(), (CtElement) susp.get(0));

								} else {

									// The next
									InsertOperation operationIns = (InsertOperation) operation;
									// lineP = MappingAnalysis.getParentLine(new LineFilter(),
									// operationIns.getParent());
									List<CtElement> follow = MappingAnalysis.getFollowStatements(diff,
											operationIns.getAction());
									if (!follow.isEmpty()) {
										lineP = follow.get(0);

									} else {
										// in case of adding at the end
										lineP = MappingAnalysis.getParentLine(new LineFilter(),
												operationIns.getParent());
									}
									susp.add(lineP);
								}

								lineTree = MappingAnalysis.getCorrespondingInSourceTree(diff,
										operation.getAction().getNode(), lineP);
								if (binaryOperator.getKind().equals(BinaryOperatorKind.EQ)) {
									repairPatterns.incrementFeatureCounterInstance(MISS_NULL_CHECK_P,
											new PatternInstance(MISS_NULL_CHECK_P, operation, parent, susp, lineP,
													lineTree));
								} else {
									repairPatterns.incrementFeatureCounterInstance(MISS_NULL_CHECK_N,
											new PatternInstance(MISS_NULL_CHECK_N, operation, parent, susp, lineP,
													lineTree));
								}
							}
						}
					}
				}
			}
		}
	}

}
