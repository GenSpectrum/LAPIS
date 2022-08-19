// Generated from ch\ethz\lapis\api\parser\VariantQuery.g4 by ANTLR 4.9.3

    package ch.ethz.lapis.api.parser;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VariantQueryParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9,
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, A=15, B=16, C=17, D=18,
		E=19, F=20, G=21, H=22, I=23, J=24, K=25, L=26, M=27, N=28, O=29, P=30,
		Q=31, R=32, S=33, T=34, U=35, V=36, W=37, X=38, Y=39, Z=40, MINUS=41,
		DOT=42, ASTERISK=43, ORF=44, NUMBER=45, WHITESPACE=46;
	public static final int
		RULE_start = 0, RULE_expr = 1, RULE_single = 2, RULE_nuc_mut = 3, RULE_aa_mut = 4,
		RULE_nuc_ins = 5, RULE_aa_ins = 6, RULE_position = 7, RULE_aa = 8, RULE_aa_mutated = 9,
		RULE_nuc = 10, RULE_nuc_mutated = 11, RULE_gene = 12, RULE_pango_query = 13,
		RULE_pango_include_sub = 14, RULE_pango_lineage = 15, RULE_pango_number_component = 16,
		RULE_nextclade_pango_lineage_prefix = 17, RULE_nextclade_pango_query = 18,
		RULE_gisaid_clade = 19, RULE_gisaid_clade_prefix = 20, RULE_gisaid_clade_query = 21,
		RULE_nextstrain_clade = 22, RULE_nextstrain_clade_prefix = 23, RULE_nextstrain_clade_query = 24,
		RULE_tree_node = 25, RULE_character = 26, RULE_n_of = 27, RULE_n_of_exactly = 28,
		RULE_n_of_n = 29, RULE_n_of_exprs = 30;
	private static String[] makeRuleNames() {
		return new String[] {
			"start", "expr", "single", "nuc_mut", "aa_mut", "nuc_ins", "aa_ins",
			"position", "aa", "aa_mutated", "nuc", "nuc_mutated", "gene", "pango_query",
			"pango_include_sub", "pango_lineage", "pango_number_component", "nextclade_pango_lineage_prefix",
			"nextclade_pango_query", "gisaid_clade", "gisaid_clade_prefix", "gisaid_clade_query",
			"nextstrain_clade", "nextstrain_clade_prefix", "nextstrain_clade_query",
			"tree_node", "character", "n_of", "n_of_exactly", "n_of_n", "n_of_exprs"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'!'", "'&'", "'|'", "'('", "')'", "':'", "'INS_'", "'?'", "'TEMPORARY_NODE_'",
			"'['", "'-OF:'", "']'", "'EXACTLY-'", "','", "'A'", "'B'", "'C'", "'D'",
			"'E'", "'F'", "'G'", "'H'", "'I'", "'J'", "'K'", "'L'", "'M'", "'N'",
			"'O'", "'P'", "'Q'", "'R'", "'S'", "'T'", "'U'", "'V'", "'W'", "'X'",
			"'Y'", "'Z'", "'-'", "'.'", "'*'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, null, "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
			"L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
			"Z", "MINUS", "DOT", "ASTERISK", "ORF", "NUMBER", "WHITESPACE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "VariantQuery.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public VariantQueryParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class StartContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(VariantQueryParser.EOF, 0); }
		public StartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_start; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitStart(this);
		}
	}

	public final StartContext start() throws RecognitionException {
		StartContext _localctx = new StartContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_start);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(62);
			expr(0);
			setState(63);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }

		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ParContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ParContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterPar(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitPar(this);
		}
	}
	public static class UniContext extends ExprContext {
		public SingleContext single() {
			return getRuleContext(SingleContext.class,0);
		}
		public UniContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterUni(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitUni(this);
		}
	}
	public static class NegContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public NegContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNeg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNeg(this);
		}
	}
	public static class OrContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public OrContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitOr(this);
		}
	}
	public static class AndContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public AndContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitAnd(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__6:
			case T__8:
			case T__9:
			case A:
			case B:
			case C:
			case D:
			case E:
			case F:
			case G:
			case H:
			case I:
			case J:
			case K:
			case L:
			case M:
			case N:
			case O:
			case P:
			case Q:
			case R:
			case S:
			case T:
			case U:
			case V:
			case W:
			case X:
			case Y:
			case Z:
			case ORF:
			case NUMBER:
				{
				_localctx = new UniContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(66);
				single();
				}
				break;
			case T__0:
				{
				_localctx = new NegContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(67);
				match(T__0);
				setState(68);
				expr(4);
				}
				break;
			case T__3:
				{
				_localctx = new ParContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(69);
				match(T__3);
				setState(70);
				expr(0);
				setState(71);
				match(T__4);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(83);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(81);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new AndContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(75);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(76);
						match(T__1);
						setState(77);
						expr(4);
						}
						break;
					case 2:
						{
						_localctx = new OrContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(78);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(79);
						match(T__2);
						setState(80);
						expr(3);
						}
						break;
					}
					}
				}
				setState(85);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class SingleContext extends ParserRuleContext {
		public Aa_mutContext aa_mut() {
			return getRuleContext(Aa_mutContext.class,0);
		}
		public Nuc_mutContext nuc_mut() {
			return getRuleContext(Nuc_mutContext.class,0);
		}
		public Nuc_insContext nuc_ins() {
			return getRuleContext(Nuc_insContext.class,0);
		}
		public Aa_insContext aa_ins() {
			return getRuleContext(Aa_insContext.class,0);
		}
		public Pango_queryContext pango_query() {
			return getRuleContext(Pango_queryContext.class,0);
		}
		public Nextclade_pango_queryContext nextclade_pango_query() {
			return getRuleContext(Nextclade_pango_queryContext.class,0);
		}
		public Gisaid_clade_queryContext gisaid_clade_query() {
			return getRuleContext(Gisaid_clade_queryContext.class,0);
		}
		public Nextstrain_clade_queryContext nextstrain_clade_query() {
			return getRuleContext(Nextstrain_clade_queryContext.class,0);
		}
		public N_ofContext n_of() {
			return getRuleContext(N_ofContext.class,0);
		}
		public Tree_nodeContext tree_node() {
			return getRuleContext(Tree_nodeContext.class,0);
		}
		public SingleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_single; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterSingle(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitSingle(this);
		}
	}

	public final SingleContext single() throws RecognitionException {
		SingleContext _localctx = new SingleContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_single);
		try {
			setState(96);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(86);
				aa_mut();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(87);
				nuc_mut();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(88);
				nuc_ins();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(89);
				aa_ins();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(90);
				pango_query();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(91);
				nextclade_pango_query();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(92);
				gisaid_clade_query();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(93);
				nextstrain_clade_query();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(94);
				n_of();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(95);
				tree_node();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nuc_mutContext extends ParserRuleContext {
		public PositionContext position() {
			return getRuleContext(PositionContext.class,0);
		}
		public NucContext nuc() {
			return getRuleContext(NucContext.class,0);
		}
		public Nuc_mutatedContext nuc_mutated() {
			return getRuleContext(Nuc_mutatedContext.class,0);
		}
		public Nuc_mutContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nuc_mut; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNuc_mut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNuc_mut(this);
		}
	}

	public final Nuc_mutContext nuc_mut() throws RecognitionException {
		Nuc_mutContext _localctx = new Nuc_mutContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_nuc_mut);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(99);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << C) | (1L << G) | (1L << T))) != 0)) {
				{
				setState(98);
				nuc();
				}
			}

			setState(101);
			position();
			setState(103);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				setState(102);
				nuc_mutated();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Aa_mutContext extends ParserRuleContext {
		public GeneContext gene() {
			return getRuleContext(GeneContext.class,0);
		}
		public PositionContext position() {
			return getRuleContext(PositionContext.class,0);
		}
		public AaContext aa() {
			return getRuleContext(AaContext.class,0);
		}
		public Aa_mutatedContext aa_mutated() {
			return getRuleContext(Aa_mutatedContext.class,0);
		}
		public Aa_mutContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aa_mut; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterAa_mut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitAa_mut(this);
		}
	}

	public final Aa_mutContext aa_mut() throws RecognitionException {
		Aa_mutContext _localctx = new Aa_mutContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_aa_mut);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(105);
			gene();
			setState(106);
			match(T__5);
			setState(108);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << C) | (1L << D) | (1L << E) | (1L << F) | (1L << G) | (1L << H) | (1L << I) | (1L << K) | (1L << L) | (1L << M) | (1L << N) | (1L << P) | (1L << Q) | (1L << R) | (1L << S) | (1L << T) | (1L << V) | (1L << W) | (1L << Y) | (1L << ASTERISK))) != 0)) {
				{
				setState(107);
				aa();
				}
			}

			setState(110);
			position();
			setState(112);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				{
				setState(111);
				aa_mutated();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nuc_insContext extends ParserRuleContext {
		public PositionContext position() {
			return getRuleContext(PositionContext.class,0);
		}
		public List<Nuc_mutatedContext> nuc_mutated() {
			return getRuleContexts(Nuc_mutatedContext.class);
		}
		public Nuc_mutatedContext nuc_mutated(int i) {
			return getRuleContext(Nuc_mutatedContext.class,i);
		}
		public Nuc_insContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nuc_ins; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNuc_ins(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNuc_ins(this);
		}
	}

	public final Nuc_insContext nuc_ins() throws RecognitionException {
		Nuc_insContext _localctx = new Nuc_insContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_nuc_ins);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
			match(T__6);
			setState(115);
			position();
			setState(116);
			match(T__5);
			setState(119);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(119);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case A:
					case B:
					case C:
					case D:
					case G:
					case H:
					case K:
					case M:
					case N:
					case R:
					case S:
					case T:
					case V:
					case W:
					case Y:
					case MINUS:
						{
						setState(117);
						nuc_mutated();
						}
						break;
					case T__7:
						{
						setState(118);
						match(T__7);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(121);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Aa_insContext extends ParserRuleContext {
		public GeneContext gene() {
			return getRuleContext(GeneContext.class,0);
		}
		public PositionContext position() {
			return getRuleContext(PositionContext.class,0);
		}
		public List<Aa_mutatedContext> aa_mutated() {
			return getRuleContexts(Aa_mutatedContext.class);
		}
		public Aa_mutatedContext aa_mutated(int i) {
			return getRuleContext(Aa_mutatedContext.class,i);
		}
		public Aa_insContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aa_ins; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterAa_ins(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitAa_ins(this);
		}
	}

	public final Aa_insContext aa_ins() throws RecognitionException {
		Aa_insContext _localctx = new Aa_insContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_aa_ins);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			match(T__6);
			setState(124);
			gene();
			setState(125);
			match(T__5);
			setState(126);
			position();
			setState(127);
			match(T__5);
			setState(130);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(130);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case A:
					case C:
					case D:
					case E:
					case F:
					case G:
					case H:
					case I:
					case K:
					case L:
					case M:
					case N:
					case P:
					case Q:
					case R:
					case S:
					case T:
					case V:
					case W:
					case X:
					case Y:
					case MINUS:
					case ASTERISK:
						{
						setState(128);
						aa_mutated();
						}
						break;
					case T__7:
						{
						setState(129);
						match(T__7);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(132);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PositionContext extends ParserRuleContext {
		public List<TerminalNode> NUMBER() { return getTokens(VariantQueryParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(VariantQueryParser.NUMBER, i);
		}
		public PositionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_position; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterPosition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitPosition(this);
		}
	}

	public final PositionContext position() throws RecognitionException {
		PositionContext _localctx = new PositionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_position);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(135);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(134);
					match(NUMBER);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(137);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AaContext extends ParserRuleContext {
		public TerminalNode A() { return getToken(VariantQueryParser.A, 0); }
		public TerminalNode R() { return getToken(VariantQueryParser.R, 0); }
		public TerminalNode N() { return getToken(VariantQueryParser.N, 0); }
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
		public TerminalNode C() { return getToken(VariantQueryParser.C, 0); }
		public TerminalNode E() { return getToken(VariantQueryParser.E, 0); }
		public TerminalNode Q() { return getToken(VariantQueryParser.Q, 0); }
		public TerminalNode G() { return getToken(VariantQueryParser.G, 0); }
		public TerminalNode H() { return getToken(VariantQueryParser.H, 0); }
		public TerminalNode I() { return getToken(VariantQueryParser.I, 0); }
		public TerminalNode L() { return getToken(VariantQueryParser.L, 0); }
		public TerminalNode K() { return getToken(VariantQueryParser.K, 0); }
		public TerminalNode M() { return getToken(VariantQueryParser.M, 0); }
		public TerminalNode F() { return getToken(VariantQueryParser.F, 0); }
		public TerminalNode P() { return getToken(VariantQueryParser.P, 0); }
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode T() { return getToken(VariantQueryParser.T, 0); }
		public TerminalNode W() { return getToken(VariantQueryParser.W, 0); }
		public TerminalNode Y() { return getToken(VariantQueryParser.Y, 0); }
		public TerminalNode V() { return getToken(VariantQueryParser.V, 0); }
		public TerminalNode ASTERISK() { return getToken(VariantQueryParser.ASTERISK, 0); }
		public AaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aa; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterAa(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitAa(this);
		}
	}

	public final AaContext aa() throws RecognitionException {
		AaContext _localctx = new AaContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_aa);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(139);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << C) | (1L << D) | (1L << E) | (1L << F) | (1L << G) | (1L << H) | (1L << I) | (1L << K) | (1L << L) | (1L << M) | (1L << N) | (1L << P) | (1L << Q) | (1L << R) | (1L << S) | (1L << T) | (1L << V) | (1L << W) | (1L << Y) | (1L << ASTERISK))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Aa_mutatedContext extends ParserRuleContext {
		public AaContext aa() {
			return getRuleContext(AaContext.class,0);
		}
		public TerminalNode X() { return getToken(VariantQueryParser.X, 0); }
		public TerminalNode MINUS() { return getToken(VariantQueryParser.MINUS, 0); }
		public Aa_mutatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aa_mutated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterAa_mutated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitAa_mutated(this);
		}
	}

	public final Aa_mutatedContext aa_mutated() throws RecognitionException {
		Aa_mutatedContext _localctx = new Aa_mutatedContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_aa_mutated);
		try {
			setState(144);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case A:
			case C:
			case D:
			case E:
			case F:
			case G:
			case H:
			case I:
			case K:
			case L:
			case M:
			case N:
			case P:
			case Q:
			case R:
			case S:
			case T:
			case V:
			case W:
			case Y:
			case ASTERISK:
				enterOuterAlt(_localctx, 1);
				{
				setState(141);
				aa();
				}
				break;
			case X:
				enterOuterAlt(_localctx, 2);
				{
				setState(142);
				match(X);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(143);
				match(MINUS);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NucContext extends ParserRuleContext {
		public TerminalNode A() { return getToken(VariantQueryParser.A, 0); }
		public TerminalNode C() { return getToken(VariantQueryParser.C, 0); }
		public TerminalNode G() { return getToken(VariantQueryParser.G, 0); }
		public TerminalNode T() { return getToken(VariantQueryParser.T, 0); }
		public NucContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nuc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNuc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNuc(this);
		}
	}

	public final NucContext nuc() throws RecognitionException {
		NucContext _localctx = new NucContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_nuc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(146);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << C) | (1L << G) | (1L << T))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nuc_mutatedContext extends ParserRuleContext {
		public NucContext nuc() {
			return getRuleContext(NucContext.class,0);
		}
		public TerminalNode M() { return getToken(VariantQueryParser.M, 0); }
		public TerminalNode R() { return getToken(VariantQueryParser.R, 0); }
		public TerminalNode W() { return getToken(VariantQueryParser.W, 0); }
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode Y() { return getToken(VariantQueryParser.Y, 0); }
		public TerminalNode K() { return getToken(VariantQueryParser.K, 0); }
		public TerminalNode V() { return getToken(VariantQueryParser.V, 0); }
		public TerminalNode H() { return getToken(VariantQueryParser.H, 0); }
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
		public TerminalNode B() { return getToken(VariantQueryParser.B, 0); }
		public TerminalNode N() { return getToken(VariantQueryParser.N, 0); }
		public TerminalNode MINUS() { return getToken(VariantQueryParser.MINUS, 0); }
		public Nuc_mutatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nuc_mutated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNuc_mutated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNuc_mutated(this);
		}
	}

	public final Nuc_mutatedContext nuc_mutated() throws RecognitionException {
		Nuc_mutatedContext _localctx = new Nuc_mutatedContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_nuc_mutated);
		try {
			setState(161);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case A:
			case C:
			case G:
			case T:
				enterOuterAlt(_localctx, 1);
				{
				setState(148);
				nuc();
				}
				break;
			case M:
				enterOuterAlt(_localctx, 2);
				{
				setState(149);
				match(M);
				}
				break;
			case R:
				enterOuterAlt(_localctx, 3);
				{
				setState(150);
				match(R);
				}
				break;
			case W:
				enterOuterAlt(_localctx, 4);
				{
				setState(151);
				match(W);
				}
				break;
			case S:
				enterOuterAlt(_localctx, 5);
				{
				setState(152);
				match(S);
				}
				break;
			case Y:
				enterOuterAlt(_localctx, 6);
				{
				setState(153);
				match(Y);
				}
				break;
			case K:
				enterOuterAlt(_localctx, 7);
				{
				setState(154);
				match(K);
				}
				break;
			case V:
				enterOuterAlt(_localctx, 8);
				{
				setState(155);
				match(V);
				}
				break;
			case H:
				enterOuterAlt(_localctx, 9);
				{
				setState(156);
				match(H);
				}
				break;
			case D:
				enterOuterAlt(_localctx, 10);
				{
				setState(157);
				match(D);
				}
				break;
			case B:
				enterOuterAlt(_localctx, 11);
				{
				setState(158);
				match(B);
				}
				break;
			case N:
				enterOuterAlt(_localctx, 12);
				{
				setState(159);
				match(N);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 13);
				{
				setState(160);
				match(MINUS);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GeneContext extends ParserRuleContext {
		public TerminalNode E() { return getToken(VariantQueryParser.E, 0); }
		public TerminalNode M() { return getToken(VariantQueryParser.M, 0); }
		public TerminalNode N() { return getToken(VariantQueryParser.N, 0); }
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode ORF() { return getToken(VariantQueryParser.ORF, 0); }
		public GeneContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gene; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterGene(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitGene(this);
		}
	}

	public final GeneContext gene() throws RecognitionException {
		GeneContext _localctx = new GeneContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_gene);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(163);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << E) | (1L << M) | (1L << N) | (1L << S) | (1L << ORF))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Pango_queryContext extends ParserRuleContext {
		public Pango_lineageContext pango_lineage() {
			return getRuleContext(Pango_lineageContext.class,0);
		}
		public Pango_include_subContext pango_include_sub() {
			return getRuleContext(Pango_include_subContext.class,0);
		}
		public Pango_queryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pango_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterPango_query(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitPango_query(this);
		}
	}

	public final Pango_queryContext pango_query() throws RecognitionException {
		Pango_queryContext _localctx = new Pango_queryContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_pango_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			pango_lineage();
			setState(167);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(166);
				pango_include_sub();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Pango_include_subContext extends ParserRuleContext {
		public TerminalNode ASTERISK() { return getToken(VariantQueryParser.ASTERISK, 0); }
		public TerminalNode DOT() { return getToken(VariantQueryParser.DOT, 0); }
		public Pango_include_subContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pango_include_sub; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterPango_include_sub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitPango_include_sub(this);
		}
	}

	public final Pango_include_subContext pango_include_sub() throws RecognitionException {
		Pango_include_subContext _localctx = new Pango_include_subContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_pango_include_sub);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(169);
				match(DOT);
				}
			}

			setState(172);
			match(ASTERISK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Pango_lineageContext extends ParserRuleContext {
		public List<CharacterContext> character() {
			return getRuleContexts(CharacterContext.class);
		}
		public CharacterContext character(int i) {
			return getRuleContext(CharacterContext.class,i);
		}
		public List<Pango_number_componentContext> pango_number_component() {
			return getRuleContexts(Pango_number_componentContext.class);
		}
		public Pango_number_componentContext pango_number_component(int i) {
			return getRuleContext(Pango_number_componentContext.class,i);
		}
		public Pango_lineageContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pango_lineage; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterPango_lineage(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitPango_lineage(this);
		}
	}

	public final Pango_lineageContext pango_lineage() throws RecognitionException {
		Pango_lineageContext _localctx = new Pango_lineageContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_pango_lineage);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(174);
			character();
			setState(176);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(175);
				character();
				}
				break;
			}
			setState(179);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(178);
				character();
				}
				break;
			}
			setState(184);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(181);
					pango_number_component();
					}
					}
				}
				setState(186);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Pango_number_componentContext extends ParserRuleContext {
		public TerminalNode DOT() { return getToken(VariantQueryParser.DOT, 0); }
		public List<TerminalNode> NUMBER() { return getTokens(VariantQueryParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(VariantQueryParser.NUMBER, i);
		}
		public Pango_number_componentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pango_number_component; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterPango_number_component(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitPango_number_component(this);
		}
	}

	public final Pango_number_componentContext pango_number_component() throws RecognitionException {
		Pango_number_componentContext _localctx = new Pango_number_componentContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_pango_number_component);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(187);
			match(DOT);
			setState(188);
			match(NUMBER);
			setState(190);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(189);
				match(NUMBER);
				}
				break;
			}
			setState(193);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(192);
				match(NUMBER);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nextclade_pango_lineage_prefixContext extends ParserRuleContext {
		public List<TerminalNode> N() { return getTokens(VariantQueryParser.N); }
		public TerminalNode N(int i) {
			return getToken(VariantQueryParser.N, i);
		}
		public List<TerminalNode> E() { return getTokens(VariantQueryParser.E); }
		public TerminalNode E(int i) {
			return getToken(VariantQueryParser.E, i);
		}
		public TerminalNode X() { return getToken(VariantQueryParser.X, 0); }
		public TerminalNode T() { return getToken(VariantQueryParser.T, 0); }
		public TerminalNode C() { return getToken(VariantQueryParser.C, 0); }
		public List<TerminalNode> L() { return getTokens(VariantQueryParser.L); }
		public TerminalNode L(int i) {
			return getToken(VariantQueryParser.L, i);
		}
		public List<TerminalNode> A() { return getTokens(VariantQueryParser.A); }
		public TerminalNode A(int i) {
			return getToken(VariantQueryParser.A, i);
		}
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
		public TerminalNode P() { return getToken(VariantQueryParser.P, 0); }
		public List<TerminalNode> G() { return getTokens(VariantQueryParser.G); }
		public TerminalNode G(int i) {
			return getToken(VariantQueryParser.G, i);
		}
		public TerminalNode O() { return getToken(VariantQueryParser.O, 0); }
		public TerminalNode I() { return getToken(VariantQueryParser.I, 0); }
		public Nextclade_pango_lineage_prefixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nextclade_pango_lineage_prefix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNextclade_pango_lineage_prefix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNextclade_pango_lineage_prefix(this);
		}
	}

	public final Nextclade_pango_lineage_prefixContext nextclade_pango_lineage_prefix() throws RecognitionException {
		Nextclade_pango_lineage_prefixContext _localctx = new Nextclade_pango_lineage_prefixContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_nextclade_pango_lineage_prefix);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(195);
			match(N);
			setState(196);
			match(E);
			setState(197);
			match(X);
			setState(198);
			match(T);
			setState(199);
			match(C);
			setState(200);
			match(L);
			setState(201);
			match(A);
			setState(202);
			match(D);
			setState(203);
			match(E);
			setState(204);
			match(P);
			setState(205);
			match(A);
			setState(206);
			match(N);
			setState(207);
			match(G);
			setState(208);
			match(O);
			setState(209);
			match(L);
			setState(210);
			match(I);
			setState(211);
			match(N);
			setState(212);
			match(E);
			setState(213);
			match(A);
			setState(214);
			match(G);
			setState(215);
			match(E);
			setState(216);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nextclade_pango_queryContext extends ParserRuleContext {
		public Nextclade_pango_lineage_prefixContext nextclade_pango_lineage_prefix() {
			return getRuleContext(Nextclade_pango_lineage_prefixContext.class,0);
		}
		public Pango_queryContext pango_query() {
			return getRuleContext(Pango_queryContext.class,0);
		}
		public Nextclade_pango_queryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nextclade_pango_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNextclade_pango_query(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNextclade_pango_query(this);
		}
	}

	public final Nextclade_pango_queryContext nextclade_pango_query() throws RecognitionException {
		Nextclade_pango_queryContext _localctx = new Nextclade_pango_queryContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_nextclade_pango_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(218);
			nextclade_pango_lineage_prefix();
			setState(219);
			pango_query();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Gisaid_cladeContext extends ParserRuleContext {
		public List<CharacterContext> character() {
			return getRuleContexts(CharacterContext.class);
		}
		public CharacterContext character(int i) {
			return getRuleContext(CharacterContext.class,i);
		}
		public Gisaid_cladeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gisaid_clade; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterGisaid_clade(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitGisaid_clade(this);
		}
	}

	public final Gisaid_cladeContext gisaid_clade() throws RecognitionException {
		Gisaid_cladeContext _localctx = new Gisaid_cladeContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_gisaid_clade);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			character();
			setState(223);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(222);
				character();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Gisaid_clade_prefixContext extends ParserRuleContext {
		public TerminalNode G() { return getToken(VariantQueryParser.G, 0); }
		public List<TerminalNode> I() { return getTokens(VariantQueryParser.I); }
		public TerminalNode I(int i) {
			return getToken(VariantQueryParser.I, i);
		}
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode A() { return getToken(VariantQueryParser.A, 0); }
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
		public Gisaid_clade_prefixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gisaid_clade_prefix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterGisaid_clade_prefix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitGisaid_clade_prefix(this);
		}
	}

	public final Gisaid_clade_prefixContext gisaid_clade_prefix() throws RecognitionException {
		Gisaid_clade_prefixContext _localctx = new Gisaid_clade_prefixContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_gisaid_clade_prefix);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(225);
			match(G);
			setState(226);
			match(I);
			setState(227);
			match(S);
			setState(228);
			match(A);
			setState(229);
			match(I);
			setState(230);
			match(D);
			setState(231);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Gisaid_clade_queryContext extends ParserRuleContext {
		public Gisaid_clade_prefixContext gisaid_clade_prefix() {
			return getRuleContext(Gisaid_clade_prefixContext.class,0);
		}
		public Gisaid_cladeContext gisaid_clade() {
			return getRuleContext(Gisaid_cladeContext.class,0);
		}
		public Gisaid_clade_queryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_gisaid_clade_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterGisaid_clade_query(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitGisaid_clade_query(this);
		}
	}

	public final Gisaid_clade_queryContext gisaid_clade_query() throws RecognitionException {
		Gisaid_clade_queryContext _localctx = new Gisaid_clade_queryContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_gisaid_clade_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(233);
			gisaid_clade_prefix();
			setState(234);
			gisaid_clade();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nextstrain_cladeContext extends ParserRuleContext {
		public List<TerminalNode> NUMBER() { return getTokens(VariantQueryParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(VariantQueryParser.NUMBER, i);
		}
		public CharacterContext character() {
			return getRuleContext(CharacterContext.class,0);
		}
		public Nextstrain_cladeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nextstrain_clade; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNextstrain_clade(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNextstrain_clade(this);
		}
	}

	public final Nextstrain_cladeContext nextstrain_clade() throws RecognitionException {
		Nextstrain_cladeContext _localctx = new Nextstrain_cladeContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_nextstrain_clade);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(236);
			match(NUMBER);
			setState(237);
			match(NUMBER);
			setState(238);
			character();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nextstrain_clade_prefixContext extends ParserRuleContext {
		public List<TerminalNode> N() { return getTokens(VariantQueryParser.N); }
		public TerminalNode N(int i) {
			return getToken(VariantQueryParser.N, i);
		}
		public List<TerminalNode> E() { return getTokens(VariantQueryParser.E); }
		public TerminalNode E(int i) {
			return getToken(VariantQueryParser.E, i);
		}
		public TerminalNode X() { return getToken(VariantQueryParser.X, 0); }
		public List<TerminalNode> T() { return getTokens(VariantQueryParser.T); }
		public TerminalNode T(int i) {
			return getToken(VariantQueryParser.T, i);
		}
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode R() { return getToken(VariantQueryParser.R, 0); }
		public List<TerminalNode> A() { return getTokens(VariantQueryParser.A); }
		public TerminalNode A(int i) {
			return getToken(VariantQueryParser.A, i);
		}
		public TerminalNode I() { return getToken(VariantQueryParser.I, 0); }
		public TerminalNode C() { return getToken(VariantQueryParser.C, 0); }
		public TerminalNode L() { return getToken(VariantQueryParser.L, 0); }
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
		public Nextstrain_clade_prefixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nextstrain_clade_prefix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNextstrain_clade_prefix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNextstrain_clade_prefix(this);
		}
	}

	public final Nextstrain_clade_prefixContext nextstrain_clade_prefix() throws RecognitionException {
		Nextstrain_clade_prefixContext _localctx = new Nextstrain_clade_prefixContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_nextstrain_clade_prefix);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(240);
			match(N);
			setState(241);
			match(E);
			setState(242);
			match(X);
			setState(243);
			match(T);
			setState(244);
			match(S);
			setState(245);
			match(T);
			setState(246);
			match(R);
			setState(247);
			match(A);
			setState(248);
			match(I);
			setState(249);
			match(N);
			setState(250);
			match(C);
			setState(251);
			match(L);
			setState(252);
			match(A);
			setState(253);
			match(D);
			setState(254);
			match(E);
			setState(255);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nextstrain_clade_queryContext extends ParserRuleContext {
		public Nextstrain_clade_prefixContext nextstrain_clade_prefix() {
			return getRuleContext(Nextstrain_clade_prefixContext.class,0);
		}
		public Nextstrain_cladeContext nextstrain_clade() {
			return getRuleContext(Nextstrain_cladeContext.class,0);
		}
		public Nextstrain_clade_queryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nextstrain_clade_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterNextstrain_clade_query(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitNextstrain_clade_query(this);
		}
	}

	public final Nextstrain_clade_queryContext nextstrain_clade_query() throws RecognitionException {
		Nextstrain_clade_queryContext _localctx = new Nextstrain_clade_queryContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_nextstrain_clade_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(257);
			nextstrain_clade_prefix();
			setState(258);
			nextstrain_clade();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tree_nodeContext extends ParserRuleContext {
		public List<TerminalNode> NUMBER() { return getTokens(VariantQueryParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(VariantQueryParser.NUMBER, i);
		}
		public Tree_nodeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tree_node; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterTree_node(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitTree_node(this);
		}
	}

	public final Tree_nodeContext tree_node() throws RecognitionException {
		Tree_nodeContext _localctx = new Tree_nodeContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_tree_node);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(260);
			match(T__8);
			setState(262);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(261);
					match(NUMBER);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(264);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,23,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CharacterContext extends ParserRuleContext {
		public TerminalNode A() { return getToken(VariantQueryParser.A, 0); }
		public TerminalNode B() { return getToken(VariantQueryParser.B, 0); }
		public TerminalNode C() { return getToken(VariantQueryParser.C, 0); }
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
		public TerminalNode E() { return getToken(VariantQueryParser.E, 0); }
		public TerminalNode F() { return getToken(VariantQueryParser.F, 0); }
		public TerminalNode G() { return getToken(VariantQueryParser.G, 0); }
		public TerminalNode H() { return getToken(VariantQueryParser.H, 0); }
		public TerminalNode I() { return getToken(VariantQueryParser.I, 0); }
		public TerminalNode J() { return getToken(VariantQueryParser.J, 0); }
		public TerminalNode K() { return getToken(VariantQueryParser.K, 0); }
		public TerminalNode L() { return getToken(VariantQueryParser.L, 0); }
		public TerminalNode M() { return getToken(VariantQueryParser.M, 0); }
		public TerminalNode N() { return getToken(VariantQueryParser.N, 0); }
		public TerminalNode O() { return getToken(VariantQueryParser.O, 0); }
		public TerminalNode P() { return getToken(VariantQueryParser.P, 0); }
		public TerminalNode Q() { return getToken(VariantQueryParser.Q, 0); }
		public TerminalNode R() { return getToken(VariantQueryParser.R, 0); }
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode T() { return getToken(VariantQueryParser.T, 0); }
		public TerminalNode U() { return getToken(VariantQueryParser.U, 0); }
		public TerminalNode V() { return getToken(VariantQueryParser.V, 0); }
		public TerminalNode W() { return getToken(VariantQueryParser.W, 0); }
		public TerminalNode X() { return getToken(VariantQueryParser.X, 0); }
		public TerminalNode Y() { return getToken(VariantQueryParser.Y, 0); }
		public TerminalNode Z() { return getToken(VariantQueryParser.Z, 0); }
		public CharacterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_character; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterCharacter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitCharacter(this);
		}
	}

	public final CharacterContext character() throws RecognitionException {
		CharacterContext _localctx = new CharacterContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_character);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(266);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << B) | (1L << C) | (1L << D) | (1L << E) | (1L << F) | (1L << G) | (1L << H) | (1L << I) | (1L << J) | (1L << K) | (1L << L) | (1L << M) | (1L << N) | (1L << O) | (1L << P) | (1L << Q) | (1L << R) | (1L << S) | (1L << T) | (1L << U) | (1L << V) | (1L << W) | (1L << X) | (1L << Y) | (1L << Z))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_ofContext extends ParserRuleContext {
		public N_of_nContext n_of_n() {
			return getRuleContext(N_of_nContext.class,0);
		}
		public N_of_exprsContext n_of_exprs() {
			return getRuleContext(N_of_exprsContext.class,0);
		}
		public N_of_exactlyContext n_of_exactly() {
			return getRuleContext(N_of_exactlyContext.class,0);
		}
		public N_ofContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_of; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterN_of(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitN_of(this);
		}
	}

	public final N_ofContext n_of() throws RecognitionException {
		N_ofContext _localctx = new N_ofContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_n_of);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(268);
			match(T__9);
			setState(270);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(269);
				n_of_exactly();
				}
			}

			setState(272);
			n_of_n();
			setState(273);
			match(T__10);
			setState(274);
			n_of_exprs();
			setState(275);
			match(T__11);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_of_exactlyContext extends ParserRuleContext {
		public N_of_exactlyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_of_exactly; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterN_of_exactly(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitN_of_exactly(this);
		}
	}

	public final N_of_exactlyContext n_of_exactly() throws RecognitionException {
		N_of_exactlyContext _localctx = new N_of_exactlyContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_n_of_exactly);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(277);
			match(T__12);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_of_nContext extends ParserRuleContext {
		public List<TerminalNode> NUMBER() { return getTokens(VariantQueryParser.NUMBER); }
		public TerminalNode NUMBER(int i) {
			return getToken(VariantQueryParser.NUMBER, i);
		}
		public N_of_nContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_of_n; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterN_of_n(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitN_of_n(this);
		}
	}

	public final N_of_nContext n_of_n() throws RecognitionException {
		N_of_nContext _localctx = new N_of_nContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_n_of_n);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(279);
				match(NUMBER);
				}
				}
				setState(282);
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class N_of_exprsContext extends ParserRuleContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public N_of_exprsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_n_of_exprs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterN_of_exprs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitN_of_exprs(this);
		}
	}

	public final N_of_exprsContext n_of_exprs() throws RecognitionException {
		N_of_exprsContext _localctx = new N_of_exprsContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_n_of_exprs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(284);
			expr(0);
			setState(289);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__13) {
				{
				{
				setState(285);
				match(T__13);
				setState(286);
				expr(0);
				}
				}
				setState(291);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\60\u0127\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \3\2"+
		"\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3L\n\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\7\3T\n\3\f\3\16\3W\13\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5"+
		"\4c\n\4\3\5\5\5f\n\5\3\5\3\5\5\5j\n\5\3\6\3\6\3\6\5\6o\n\6\3\6\3\6\5\6"+
		"s\n\6\3\7\3\7\3\7\3\7\3\7\6\7z\n\7\r\7\16\7{\3\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\6\b\u0085\n\b\r\b\16\b\u0086\3\t\6\t\u008a\n\t\r\t\16\t\u008b\3\n\3"+
		"\n\3\13\3\13\3\13\5\13\u0093\n\13\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u00a4\n\r\3\16\3\16\3\17\3\17\5\17\u00aa"+
		"\n\17\3\20\5\20\u00ad\n\20\3\20\3\20\3\21\3\21\5\21\u00b3\n\21\3\21\5"+
		"\21\u00b6\n\21\3\21\7\21\u00b9\n\21\f\21\16\21\u00bc\13\21\3\22\3\22\3"+
		"\22\5\22\u00c1\n\22\3\22\5\22\u00c4\n\22\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\24\3\24\3\24\3\25\3\25\5\25\u00e2\n\25\3\26\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\32\3\32\3\32\3\33\3\33\6\33\u0109\n\33\r\33\16\33\u010a\3\34\3"+
		"\34\3\35\3\35\5\35\u0111\n\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\37"+
		"\6\37\u011b\n\37\r\37\16\37\u011c\3 \3 \3 \7 \u0122\n \f \16 \u0125\13"+
		" \3 \2\3\4!\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\66"+
		"8:<>\2\6\t\2\21\21\23\31\33\36 $&\'))--\6\2\21\21\23\23\27\27$$\6\2\25"+
		"\25\35\36##..\3\2\21*\2\u0137\2@\3\2\2\2\4K\3\2\2\2\6b\3\2\2\2\be\3\2"+
		"\2\2\nk\3\2\2\2\ft\3\2\2\2\16}\3\2\2\2\20\u0089\3\2\2\2\22\u008d\3\2\2"+
		"\2\24\u0092\3\2\2\2\26\u0094\3\2\2\2\30\u00a3\3\2\2\2\32\u00a5\3\2\2\2"+
		"\34\u00a7\3\2\2\2\36\u00ac\3\2\2\2 \u00b0\3\2\2\2\"\u00bd\3\2\2\2$\u00c5"+
		"\3\2\2\2&\u00dc\3\2\2\2(\u00df\3\2\2\2*\u00e3\3\2\2\2,\u00eb\3\2\2\2."+
		"\u00ee\3\2\2\2\60\u00f2\3\2\2\2\62\u0103\3\2\2\2\64\u0106\3\2\2\2\66\u010c"+
		"\3\2\2\28\u010e\3\2\2\2:\u0117\3\2\2\2<\u011a\3\2\2\2>\u011e\3\2\2\2@"+
		"A\5\4\3\2AB\7\2\2\3B\3\3\2\2\2CD\b\3\1\2DL\5\6\4\2EF\7\3\2\2FL\5\4\3\6"+
		"GH\7\6\2\2HI\5\4\3\2IJ\7\7\2\2JL\3\2\2\2KC\3\2\2\2KE\3\2\2\2KG\3\2\2\2"+
		"LU\3\2\2\2MN\f\5\2\2NO\7\4\2\2OT\5\4\3\6PQ\f\4\2\2QR\7\5\2\2RT\5\4\3\5"+
		"SM\3\2\2\2SP\3\2\2\2TW\3\2\2\2US\3\2\2\2UV\3\2\2\2V\5\3\2\2\2WU\3\2\2"+
		"\2Xc\5\n\6\2Yc\5\b\5\2Zc\5\f\7\2[c\5\16\b\2\\c\5\34\17\2]c\5&\24\2^c\5"+
		",\27\2_c\5\62\32\2`c\58\35\2ac\5\64\33\2bX\3\2\2\2bY\3\2\2\2bZ\3\2\2\2"+
		"b[\3\2\2\2b\\\3\2\2\2b]\3\2\2\2b^\3\2\2\2b_\3\2\2\2b`\3\2\2\2ba\3\2\2"+
		"\2c\7\3\2\2\2df\5\26\f\2ed\3\2\2\2ef\3\2\2\2fg\3\2\2\2gi\5\20\t\2hj\5"+
		"\30\r\2ih\3\2\2\2ij\3\2\2\2j\t\3\2\2\2kl\5\32\16\2ln\7\b\2\2mo\5\22\n"+
		"\2nm\3\2\2\2no\3\2\2\2op\3\2\2\2pr\5\20\t\2qs\5\24\13\2rq\3\2\2\2rs\3"+
		"\2\2\2s\13\3\2\2\2tu\7\t\2\2uv\5\20\t\2vy\7\b\2\2wz\5\30\r\2xz\7\n\2\2"+
		"yw\3\2\2\2yx\3\2\2\2z{\3\2\2\2{y\3\2\2\2{|\3\2\2\2|\r\3\2\2\2}~\7\t\2"+
		"\2~\177\5\32\16\2\177\u0080\7\b\2\2\u0080\u0081\5\20\t\2\u0081\u0084\7"+
		"\b\2\2\u0082\u0085\5\24\13\2\u0083\u0085\7\n\2\2\u0084\u0082\3\2\2\2\u0084"+
		"\u0083\3\2\2\2\u0085\u0086\3\2\2\2\u0086\u0084\3\2\2\2\u0086\u0087\3\2"+
		"\2\2\u0087\17\3\2\2\2\u0088\u008a\7/\2\2\u0089\u0088\3\2\2\2\u008a\u008b"+
		"\3\2\2\2\u008b\u0089\3\2\2\2\u008b\u008c\3\2\2\2\u008c\21\3\2\2\2\u008d"+
		"\u008e\t\2\2\2\u008e\23\3\2\2\2\u008f\u0093\5\22\n\2\u0090\u0093\7(\2"+
		"\2\u0091\u0093\7+\2\2\u0092\u008f\3\2\2\2\u0092\u0090\3\2\2\2\u0092\u0091"+
		"\3\2\2\2\u0093\25\3\2\2\2\u0094\u0095\t\3\2\2\u0095\27\3\2\2\2\u0096\u00a4"+
		"\5\26\f\2\u0097\u00a4\7\35\2\2\u0098\u00a4\7\"\2\2\u0099\u00a4\7\'\2\2"+
		"\u009a\u00a4\7#\2\2\u009b\u00a4\7)\2\2\u009c\u00a4\7\33\2\2\u009d\u00a4"+
		"\7&\2\2\u009e\u00a4\7\30\2\2\u009f\u00a4\7\24\2\2\u00a0\u00a4\7\22\2\2"+
		"\u00a1\u00a4\7\36\2\2\u00a2\u00a4\7+\2\2\u00a3\u0096\3\2\2\2\u00a3\u0097"+
		"\3\2\2\2\u00a3\u0098\3\2\2\2\u00a3\u0099\3\2\2\2\u00a3\u009a\3\2\2\2\u00a3"+
		"\u009b\3\2\2\2\u00a3\u009c\3\2\2\2\u00a3\u009d\3\2\2\2\u00a3\u009e\3\2"+
		"\2\2\u00a3\u009f\3\2\2\2\u00a3\u00a0\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a3"+
		"\u00a2\3\2\2\2\u00a4\31\3\2\2\2\u00a5\u00a6\t\4\2\2\u00a6\33\3\2\2\2\u00a7"+
		"\u00a9\5 \21\2\u00a8\u00aa\5\36\20\2\u00a9\u00a8\3\2\2\2\u00a9\u00aa\3"+
		"\2\2\2\u00aa\35\3\2\2\2\u00ab\u00ad\7,\2\2\u00ac\u00ab\3\2\2\2\u00ac\u00ad"+
		"\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae\u00af\7-\2\2\u00af\37\3\2\2\2\u00b0"+
		"\u00b2\5\66\34\2\u00b1\u00b3\5\66\34\2\u00b2\u00b1\3\2\2\2\u00b2\u00b3"+
		"\3\2\2\2\u00b3\u00b5\3\2\2\2\u00b4\u00b6\5\66\34\2\u00b5\u00b4\3\2\2\2"+
		"\u00b5\u00b6\3\2\2\2\u00b6\u00ba\3\2\2\2\u00b7\u00b9\5\"\22\2\u00b8\u00b7"+
		"\3\2\2\2\u00b9\u00bc\3\2\2\2\u00ba\u00b8\3\2\2\2\u00ba\u00bb\3\2\2\2\u00bb"+
		"!\3\2\2\2\u00bc\u00ba\3\2\2\2\u00bd\u00be\7,\2\2\u00be\u00c0\7/\2\2\u00bf"+
		"\u00c1\7/\2\2\u00c0\u00bf\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1\u00c3\3\2"+
		"\2\2\u00c2\u00c4\7/\2\2\u00c3\u00c2\3\2\2\2\u00c3\u00c4\3\2\2\2\u00c4"+
		"#\3\2\2\2\u00c5\u00c6\7\36\2\2\u00c6\u00c7\7\25\2\2\u00c7\u00c8\7(\2\2"+
		"\u00c8\u00c9\7$\2\2\u00c9\u00ca\7\23\2\2\u00ca\u00cb\7\34\2\2\u00cb\u00cc"+
		"\7\21\2\2\u00cc\u00cd\7\24\2\2\u00cd\u00ce\7\25\2\2\u00ce\u00cf\7 \2\2"+
		"\u00cf\u00d0\7\21\2\2\u00d0\u00d1\7\36\2\2\u00d1\u00d2\7\27\2\2\u00d2"+
		"\u00d3\7\37\2\2\u00d3\u00d4\7\34\2\2\u00d4\u00d5\7\31\2\2\u00d5\u00d6"+
		"\7\36\2\2\u00d6\u00d7\7\25\2\2\u00d7\u00d8\7\21\2\2\u00d8\u00d9\7\27\2"+
		"\2\u00d9\u00da\7\25\2\2\u00da\u00db\7\b\2\2\u00db%\3\2\2\2\u00dc\u00dd"+
		"\5$\23\2\u00dd\u00de\5\34\17\2\u00de\'\3\2\2\2\u00df\u00e1\5\66\34\2\u00e0"+
		"\u00e2\5\66\34\2\u00e1\u00e0\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2)\3\2\2"+
		"\2\u00e3\u00e4\7\27\2\2\u00e4\u00e5\7\31\2\2\u00e5\u00e6\7#\2\2\u00e6"+
		"\u00e7\7\21\2\2\u00e7\u00e8\7\31\2\2\u00e8\u00e9\7\24\2\2\u00e9\u00ea"+
		"\7\b\2\2\u00ea+\3\2\2\2\u00eb\u00ec\5*\26\2\u00ec\u00ed\5(\25\2\u00ed"+
		"-\3\2\2\2\u00ee\u00ef\7/\2\2\u00ef\u00f0\7/\2\2\u00f0\u00f1\5\66\34\2"+
		"\u00f1/\3\2\2\2\u00f2\u00f3\7\36\2\2\u00f3\u00f4\7\25\2\2\u00f4\u00f5"+
		"\7(\2\2\u00f5\u00f6\7$\2\2\u00f6\u00f7\7#\2\2\u00f7\u00f8\7$\2\2\u00f8"+
		"\u00f9\7\"\2\2\u00f9\u00fa\7\21\2\2\u00fa\u00fb\7\31\2\2\u00fb\u00fc\7"+
		"\36\2\2\u00fc\u00fd\7\23\2\2\u00fd\u00fe\7\34\2\2\u00fe\u00ff\7\21\2\2"+
		"\u00ff\u0100\7\24\2\2\u0100\u0101\7\25\2\2\u0101\u0102\7\b\2\2\u0102\61"+
		"\3\2\2\2\u0103\u0104\5\60\31\2\u0104\u0105\5.\30\2\u0105\63\3\2\2\2\u0106"+
		"\u0108\7\13\2\2\u0107\u0109\7/\2\2\u0108\u0107\3\2\2\2\u0109\u010a\3\2"+
		"\2\2\u010a\u0108\3\2\2\2\u010a\u010b\3\2\2\2\u010b\65\3\2\2\2\u010c\u010d"+
		"\t\5\2\2\u010d\67\3\2\2\2\u010e\u0110\7\f\2\2\u010f\u0111\5:\36\2\u0110"+
		"\u010f\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u0112\3\2\2\2\u0112\u0113\5<"+
		"\37\2\u0113\u0114\7\r\2\2\u0114\u0115\5> \2\u0115\u0116\7\16\2\2\u0116"+
		"9\3\2\2\2\u0117\u0118\7\17\2\2\u0118;\3\2\2\2\u0119\u011b\7/\2\2\u011a"+
		"\u0119\3\2\2\2\u011b\u011c\3\2\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2"+
		"\2\2\u011d=\3\2\2\2\u011e\u0123\5\4\3\2\u011f\u0120\7\20\2\2\u0120\u0122"+
		"\5\4\3\2\u0121\u011f\3\2\2\2\u0122\u0125\3\2\2\2\u0123\u0121\3\2\2\2\u0123"+
		"\u0124\3\2\2\2\u0124?\3\2\2\2\u0125\u0123\3\2\2\2\35KSUbeinry{\u0084\u0086"+
		"\u008b\u0092\u00a3\u00a9\u00ac\u00b2\u00b5\u00ba\u00c0\u00c3\u00e1\u010a"+
		"\u0110\u011c\u0123";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
