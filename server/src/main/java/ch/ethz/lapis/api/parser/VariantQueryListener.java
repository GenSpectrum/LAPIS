// Generated from ch\ethz\lapis\api\parser\VariantQuery.g4 by ANTLR 4.9.3

    package ch.ethz.lapis.api.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link VariantQueryParser}.
 */
public interface VariantQueryListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(VariantQueryParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(VariantQueryParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#single}.
	 * @param ctx the parse tree
	 */
	void enterSingle(VariantQueryParser.SingleContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#single}.
	 * @param ctx the parse tree
	 */
	void exitSingle(VariantQueryParser.SingleContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#nuc_mut}.
	 * @param ctx the parse tree
	 */
	void enterNuc_mut(VariantQueryParser.Nuc_mutContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#nuc_mut}.
	 * @param ctx the parse tree
	 */
	void exitNuc_mut(VariantQueryParser.Nuc_mutContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#aa_mut}.
	 * @param ctx the parse tree
	 */
	void enterAa_mut(VariantQueryParser.Aa_mutContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#aa_mut}.
	 * @param ctx the parse tree
	 */
	void exitAa_mut(VariantQueryParser.Aa_mutContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#position}.
	 * @param ctx the parse tree
	 */
	void enterPosition(VariantQueryParser.PositionContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#position}.
	 * @param ctx the parse tree
	 */
	void exitPosition(VariantQueryParser.PositionContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#aa}.
	 * @param ctx the parse tree
	 */
	void enterAa(VariantQueryParser.AaContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#aa}.
	 * @param ctx the parse tree
	 */
	void exitAa(VariantQueryParser.AaContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#aa_mutated}.
	 * @param ctx the parse tree
	 */
	void enterAa_mutated(VariantQueryParser.Aa_mutatedContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#aa_mutated}.
	 * @param ctx the parse tree
	 */
	void exitAa_mutated(VariantQueryParser.Aa_mutatedContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#nuc}.
	 * @param ctx the parse tree
	 */
	void enterNuc(VariantQueryParser.NucContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#nuc}.
	 * @param ctx the parse tree
	 */
	void exitNuc(VariantQueryParser.NucContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#nuc_mutated}.
	 * @param ctx the parse tree
	 */
	void enterNuc_mutated(VariantQueryParser.Nuc_mutatedContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#nuc_mutated}.
	 * @param ctx the parse tree
	 */
	void exitNuc_mutated(VariantQueryParser.Nuc_mutatedContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#gene}.
	 * @param ctx the parse tree
	 */
	void enterGene(VariantQueryParser.GeneContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#gene}.
	 * @param ctx the parse tree
	 */
	void exitGene(VariantQueryParser.GeneContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#pango_query}.
	 * @param ctx the parse tree
	 */
	void enterPango_query(VariantQueryParser.Pango_queryContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#pango_query}.
	 * @param ctx the parse tree
	 */
	void exitPango_query(VariantQueryParser.Pango_queryContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#pango_include_sub}.
	 * @param ctx the parse tree
	 */
	void enterPango_include_sub(VariantQueryParser.Pango_include_subContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#pango_include_sub}.
	 * @param ctx the parse tree
	 */
	void exitPango_include_sub(VariantQueryParser.Pango_include_subContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#pango_lineage}.
	 * @param ctx the parse tree
	 */
	void enterPango_lineage(VariantQueryParser.Pango_lineageContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#pango_lineage}.
	 * @param ctx the parse tree
	 */
	void exitPango_lineage(VariantQueryParser.Pango_lineageContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#pango_number_component}.
	 * @param ctx the parse tree
	 */
	void enterPango_number_component(VariantQueryParser.Pango_number_componentContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#pango_number_component}.
	 * @param ctx the parse tree
	 */
	void exitPango_number_component(VariantQueryParser.Pango_number_componentContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#gisaid_clade}.
	 * @param ctx the parse tree
	 */
	void enterGisaid_clade(VariantQueryParser.Gisaid_cladeContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#gisaid_clade}.
	 * @param ctx the parse tree
	 */
	void exitGisaid_clade(VariantQueryParser.Gisaid_cladeContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#nextstrain_clade}.
	 * @param ctx the parse tree
	 */
	void enterNextstrain_clade(VariantQueryParser.Nextstrain_cladeContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#nextstrain_clade}.
	 * @param ctx the parse tree
	 */
	void exitNextstrain_clade(VariantQueryParser.Nextstrain_cladeContext ctx);
	/**
	 * Enter a parse tree produced by {@link VariantQueryParser#character}.
	 * @param ctx the parse tree
	 */
	void enterCharacter(VariantQueryParser.CharacterContext ctx);
	/**
	 * Exit a parse tree produced by {@link VariantQueryParser#character}.
	 * @param ctx the parse tree
	 */
	void exitCharacter(VariantQueryParser.CharacterContext ctx);
}