// Generated from java-escape by ANTLR 4.11.1

    package ch.ethz.lapis.api.parser;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class VariantQueryParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9,
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, A=16, B=17,
		C=18, D=19, E=20, F=21, G=22, H=23, I=24, J=25, K=26, L=27, M=28, N=29,
		O=30, P=31, Q=32, R=33, S=34, T=35, U=36, V=37, W=38, X=39, Y=40, Z=41,
		MINUS=42, DOT=43, ASTERISK=44, ORF=45, NUMBER=46, WHITESPACE=47;
	public static final int
		RULE_start = 0, RULE_expr = 1, RULE_single = 2, RULE_nuc_mut = 3, RULE_aa_mut = 4,
		RULE_nuc_ins = 5, RULE_aa_ins = 6, RULE_position = 7, RULE_aa = 8, RULE_aa_mutated = 9,
		RULE_nuc = 10, RULE_nuc_mutated = 11, RULE_gene = 12, RULE_pango_query = 13,
		RULE_pango_include_sub = 14, RULE_pango_lineage = 15, RULE_pango_number_component = 16,
		RULE_nextclade_pango_lineage_prefix = 17, RULE_nextclade_pango_query = 18,
		RULE_gisaid_clade = 19, RULE_gisaid_clade_prefix = 20, RULE_gisaid_clade_query = 21,
		RULE_nextstrain_clade = 22, RULE_nextstrain_clade_prefix = 23, RULE_nextstrain_clade_query = 24,
		RULE_character = 25, RULE_n_of = 26, RULE_n_of_exactly = 27, RULE_n_of_n = 28,
		RULE_n_of_exprs = 29;
	private static String[] makeRuleNames() {
		return new String[] {
			"start", "expr", "single", "nuc_mut", "aa_mut", "nuc_ins", "aa_ins",
			"position", "aa", "aa_mutated", "nuc", "nuc_mutated", "gene", "pango_query",
			"pango_include_sub", "pango_lineage", "pango_number_component", "nextclade_pango_lineage_prefix",
			"nextclade_pango_query", "gisaid_clade", "gisaid_clade_prefix", "gisaid_clade_query",
			"nextstrain_clade", "nextstrain_clade_prefix", "nextstrain_clade_query",
			"character", "n_of", "n_of_exactly", "n_of_n", "n_of_exprs"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'!'", "'&'", "'|'", "'('", "')'", "'MAYBE('", "':'", "'INS_'",
			"'?'", "'RECOMBINANT'", "'['", "'-OF:'", "']'", "'EXACTLY-'", "','",
			"'A'", "'B'", "'C'", "'D'", "'E'", "'F'", "'G'", "'H'", "'I'", "'J'",
			"'K'", "'L'", "'M'", "'N'", "'O'", "'P'", "'Q'", "'R'", "'S'", "'T'",
			"'U'", "'V'", "'W'", "'X'", "'Y'", "'Z'", "'-'", "'.'", "'*'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, "A", "B", "C", "D", "E", "F", "G", "H", "I",
			"J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W",
			"X", "Y", "Z", "MINUS", "DOT", "ASTERISK", "ORF", "NUMBER", "WHITESPACE"
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
	public String getGrammarFileName() { return "java-escape"; }

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

	@SuppressWarnings("CheckReturnValue")
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
			setState(60);
			expr(0);
			setState(61);
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

	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
	public static class MaybeContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public MaybeContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterMaybe(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitMaybe(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
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
	@SuppressWarnings("CheckReturnValue")
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
			setState(75);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__7:
			case T__10:
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

				setState(64);
				single();
				}
				break;
			case T__0:
				{
				_localctx = new NegContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(65);
				match(T__0);
				setState(66);
				expr(5);
				}
				break;
			case T__3:
				{
				_localctx = new ParContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(67);
				match(T__3);
				setState(68);
				expr(0);
				setState(69);
				match(T__4);
				}
				break;
			case T__5:
				{
				_localctx = new MaybeContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(71);
				match(T__5);
				setState(72);
				expr(0);
				setState(73);
				match(T__4);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(85);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(83);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new AndContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(77);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(78);
						match(T__1);
						setState(79);
						expr(5);
						}
						break;
					case 2:
						{
						_localctx = new OrContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(80);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(81);
						match(T__2);
						setState(82);
						expr(4);
						}
						break;
					}
					}
				}
				setState(87);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(97);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(88);
				aa_mut();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(89);
				nuc_mut();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(90);
				nuc_ins();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(91);
				aa_ins();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(92);
				pango_query();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(93);
				nextclade_pango_query();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(94);
				gisaid_clade_query();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(95);
				nextstrain_clade_query();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(96);
				n_of();
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(100);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 34364260352L) != 0) {
				{
				setState(99);
				nuc();
				}
			}

			setState(102);
			position();
			setState(104);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				setState(103);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(106);
			gene();
			setState(107);
			match(T__6);
			setState(109);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 19171626516480L) != 0) {
				{
				setState(108);
				aa();
				}
			}

			setState(111);
			position();
			setState(113);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				{
				setState(112);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(115);
			match(T__7);
			setState(116);
			position();
			setState(117);
			match(T__6);
			setState(120);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(120);
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
					case DOT:
						{
						setState(118);
						nuc_mutated();
						}
						break;
					case T__8:
						{
						setState(119);
						match(T__8);
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
				setState(122);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(124);
			match(T__7);
			setState(125);
			gene();
			setState(126);
			match(T__6);
			setState(127);
			position();
			setState(128);
			match(T__6);
			setState(131);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(131);
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
					case DOT:
					case ASTERISK:
						{
						setState(129);
						aa_mutated();
						}
						break;
					case T__8:
						{
						setState(130);
						match(T__8);
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
				setState(133);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(136);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(135);
					match(NUMBER);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(138);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(140);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 19171626516480L) != 0) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class Aa_mutatedContext extends ParserRuleContext {
		public AaContext aa() {
			return getRuleContext(AaContext.class,0);
		}
		public TerminalNode X() { return getToken(VariantQueryParser.X, 0); }
		public TerminalNode MINUS() { return getToken(VariantQueryParser.MINUS, 0); }
		public TerminalNode DOT() { return getToken(VariantQueryParser.DOT, 0); }
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
			setState(146);
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
				setState(142);
				aa();
				}
				break;
			case X:
				enterOuterAlt(_localctx, 2);
				{
				setState(143);
				match(X);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(144);
				match(MINUS);
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 4);
				{
				setState(145);
				match(DOT);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(148);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 34364260352L) != 0) ) {
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

	@SuppressWarnings("CheckReturnValue")
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
		public TerminalNode DOT() { return getToken(VariantQueryParser.DOT, 0); }
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
			setState(164);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case A:
			case C:
			case G:
			case T:
				enterOuterAlt(_localctx, 1);
				{
				setState(150);
				nuc();
				}
				break;
			case M:
				enterOuterAlt(_localctx, 2);
				{
				setState(151);
				match(M);
				}
				break;
			case R:
				enterOuterAlt(_localctx, 3);
				{
				setState(152);
				match(R);
				}
				break;
			case W:
				enterOuterAlt(_localctx, 4);
				{
				setState(153);
				match(W);
				}
				break;
			case S:
				enterOuterAlt(_localctx, 5);
				{
				setState(154);
				match(S);
				}
				break;
			case Y:
				enterOuterAlt(_localctx, 6);
				{
				setState(155);
				match(Y);
				}
				break;
			case K:
				enterOuterAlt(_localctx, 7);
				{
				setState(156);
				match(K);
				}
				break;
			case V:
				enterOuterAlt(_localctx, 8);
				{
				setState(157);
				match(V);
				}
				break;
			case H:
				enterOuterAlt(_localctx, 9);
				{
				setState(158);
				match(H);
				}
				break;
			case D:
				enterOuterAlt(_localctx, 10);
				{
				setState(159);
				match(D);
				}
				break;
			case B:
				enterOuterAlt(_localctx, 11);
				{
				setState(160);
				match(B);
				}
				break;
			case N:
				enterOuterAlt(_localctx, 12);
				{
				setState(161);
				match(N);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 13);
				{
				setState(162);
				match(MINUS);
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 14);
				{
				setState(163);
				match(DOT);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(166);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 35202358312960L) != 0) ) {
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(168);
			pango_lineage();
			setState(170);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(169);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(173);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(172);
				match(DOT);
				}
			}

			setState(175);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(177);
			character();
			setState(179);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(178);
				character();
				}
				break;
			}
			setState(182);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(181);
				character();
				}
				break;
			}
			setState(187);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(184);
					pango_number_component();
					}
					}
				}
				setState(189);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(190);
			match(DOT);
			setState(191);
			match(NUMBER);
			setState(193);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(192);
				match(NUMBER);
				}
				break;
			}
			setState(196);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(195);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(198);
			match(N);
			setState(199);
			match(E);
			setState(200);
			match(X);
			setState(201);
			match(T);
			setState(202);
			match(C);
			setState(203);
			match(L);
			setState(204);
			match(A);
			setState(205);
			match(D);
			setState(206);
			match(E);
			setState(207);
			match(P);
			setState(208);
			match(A);
			setState(209);
			match(N);
			setState(210);
			match(G);
			setState(211);
			match(O);
			setState(212);
			match(L);
			setState(213);
			match(I);
			setState(214);
			match(N);
			setState(215);
			match(E);
			setState(216);
			match(A);
			setState(217);
			match(G);
			setState(218);
			match(E);
			setState(219);
			match(T__6);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(221);
			nextclade_pango_lineage_prefix();
			setState(222);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(224);
			character();
			setState(226);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(225);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(228);
			match(G);
			setState(229);
			match(I);
			setState(230);
			match(S);
			setState(231);
			match(A);
			setState(232);
			match(I);
			setState(233);
			match(D);
			setState(234);
			match(T__6);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(236);
			gisaid_clade_prefix();
			setState(237);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(243);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NUMBER:
				enterOuterAlt(_localctx, 1);
				{
				setState(239);
				match(NUMBER);
				setState(240);
				match(NUMBER);
				setState(241);
				character();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(242);
				match(T__9);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(245);
			match(N);
			setState(246);
			match(E);
			setState(247);
			match(X);
			setState(248);
			match(T);
			setState(249);
			match(S);
			setState(250);
			match(T);
			setState(251);
			match(R);
			setState(252);
			match(A);
			setState(253);
			match(I);
			setState(254);
			match(N);
			setState(255);
			match(C);
			setState(256);
			match(L);
			setState(257);
			match(A);
			setState(258);
			match(D);
			setState(259);
			match(E);
			setState(260);
			match(T__6);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(262);
			nextstrain_clade_prefix();
			setState(263);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 50, RULE_character);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 4398046445568L) != 0) ) {
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 52, RULE_n_of);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(267);
			match(T__10);
			setState(269);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__13) {
				{
				setState(268);
				n_of_exactly();
				}
			}

			setState(271);
			n_of_n();
			setState(272);
			match(T__11);
			setState(273);
			n_of_exprs();
			setState(274);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 54, RULE_n_of_exactly);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(276);
			match(T__13);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 56, RULE_n_of_n);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(278);
				match(NUMBER);
				}
				}
				setState(281);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 58, RULE_n_of_exprs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(283);
			expr(0);
			setState(288);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__14) {
				{
				{
				setState(284);
				match(T__14);
				setState(285);
				expr(0);
				}
				}
				setState(290);
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
			return precpred(_ctx, 4);
		case 1:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001/\u0124\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0003\u0001L\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001T\b\u0001\n\u0001\f\u0001"+
		"W\t\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002b\b\u0002"+
		"\u0001\u0003\u0003\u0003e\b\u0003\u0001\u0003\u0001\u0003\u0003\u0003"+
		"i\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004n\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004r\b\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0005\u0004\u0005y\b\u0005\u000b\u0005\f\u0005"+
		"z\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0004\u0006\u0084\b\u0006\u000b\u0006\f\u0006\u0085\u0001"+
		"\u0007\u0004\u0007\u0089\b\u0007\u000b\u0007\f\u0007\u008a\u0001\b\u0001"+
		"\b\u0001\t\u0001\t\u0001\t\u0001\t\u0003\t\u0093\b\t\u0001\n\u0001\n\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0003\u000b\u00a5\b\u000b\u0001\f\u0001\f\u0001\r\u0001"+
		"\r\u0003\r\u00ab\b\r\u0001\u000e\u0003\u000e\u00ae\b\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000f\u0001\u000f\u0003\u000f\u00b4\b\u000f\u0001\u000f"+
		"\u0003\u000f\u00b7\b\u000f\u0001\u000f\u0005\u000f\u00ba\b\u000f\n\u000f"+
		"\f\u000f\u00bd\t\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010"+
		"\u00c2\b\u0010\u0001\u0010\u0003\u0010\u00c5\b\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0013\u0001\u0013\u0003\u0013\u00e3\b\u0013\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0003\u0016\u00f4\b\u0016\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001"+
		"\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0003\u001a\u010e\b\u001a\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001b\u0001"+
		"\u001b\u0001\u001c\u0004\u001c\u0118\b\u001c\u000b\u001c\f\u001c\u0119"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0005\u001d\u011f\b\u001d\n\u001d"+
		"\f\u001d\u0122\t\u001d\u0001\u001d\u0000\u0001\u0002\u001e\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*,.02468:\u0000\u0004\u0007\u0000\u0010\u0010\u0012\u0018\u001a"+
		"\u001d\u001f#%&((,,\u0004\u0000\u0010\u0010\u0012\u0012\u0016\u0016##"+
		"\u0004\u0000\u0014\u0014\u001c\u001d\"\"--\u0001\u0000\u0010)\u0137\u0000"+
		"<\u0001\u0000\u0000\u0000\u0002K\u0001\u0000\u0000\u0000\u0004a\u0001"+
		"\u0000\u0000\u0000\u0006d\u0001\u0000\u0000\u0000\bj\u0001\u0000\u0000"+
		"\u0000\ns\u0001\u0000\u0000\u0000\f|\u0001\u0000\u0000\u0000\u000e\u0088"+
		"\u0001\u0000\u0000\u0000\u0010\u008c\u0001\u0000\u0000\u0000\u0012\u0092"+
		"\u0001\u0000\u0000\u0000\u0014\u0094\u0001\u0000\u0000\u0000\u0016\u00a4"+
		"\u0001\u0000\u0000\u0000\u0018\u00a6\u0001\u0000\u0000\u0000\u001a\u00a8"+
		"\u0001\u0000\u0000\u0000\u001c\u00ad\u0001\u0000\u0000\u0000\u001e\u00b1"+
		"\u0001\u0000\u0000\u0000 \u00be\u0001\u0000\u0000\u0000\"\u00c6\u0001"+
		"\u0000\u0000\u0000$\u00dd\u0001\u0000\u0000\u0000&\u00e0\u0001\u0000\u0000"+
		"\u0000(\u00e4\u0001\u0000\u0000\u0000*\u00ec\u0001\u0000\u0000\u0000,"+
		"\u00f3\u0001\u0000\u0000\u0000.\u00f5\u0001\u0000\u0000\u00000\u0106\u0001"+
		"\u0000\u0000\u00002\u0109\u0001\u0000\u0000\u00004\u010b\u0001\u0000\u0000"+
		"\u00006\u0114\u0001\u0000\u0000\u00008\u0117\u0001\u0000\u0000\u0000:"+
		"\u011b\u0001\u0000\u0000\u0000<=\u0003\u0002\u0001\u0000=>\u0005\u0000"+
		"\u0000\u0001>\u0001\u0001\u0000\u0000\u0000?@\u0006\u0001\uffff\uffff"+
		"\u0000@L\u0003\u0004\u0002\u0000AB\u0005\u0001\u0000\u0000BL\u0003\u0002"+
		"\u0001\u0005CD\u0005\u0004\u0000\u0000DE\u0003\u0002\u0001\u0000EF\u0005"+
		"\u0005\u0000\u0000FL\u0001\u0000\u0000\u0000GH\u0005\u0006\u0000\u0000"+
		"HI\u0003\u0002\u0001\u0000IJ\u0005\u0005\u0000\u0000JL\u0001\u0000\u0000"+
		"\u0000K?\u0001\u0000\u0000\u0000KA\u0001\u0000\u0000\u0000KC\u0001\u0000"+
		"\u0000\u0000KG\u0001\u0000\u0000\u0000LU\u0001\u0000\u0000\u0000MN\n\u0004"+
		"\u0000\u0000NO\u0005\u0002\u0000\u0000OT\u0003\u0002\u0001\u0005PQ\n\u0003"+
		"\u0000\u0000QR\u0005\u0003\u0000\u0000RT\u0003\u0002\u0001\u0004SM\u0001"+
		"\u0000\u0000\u0000SP\u0001\u0000\u0000\u0000TW\u0001\u0000\u0000\u0000"+
		"US\u0001\u0000\u0000\u0000UV\u0001\u0000\u0000\u0000V\u0003\u0001\u0000"+
		"\u0000\u0000WU\u0001\u0000\u0000\u0000Xb\u0003\b\u0004\u0000Yb\u0003\u0006"+
		"\u0003\u0000Zb\u0003\n\u0005\u0000[b\u0003\f\u0006\u0000\\b\u0003\u001a"+
		"\r\u0000]b\u0003$\u0012\u0000^b\u0003*\u0015\u0000_b\u00030\u0018\u0000"+
		"`b\u00034\u001a\u0000aX\u0001\u0000\u0000\u0000aY\u0001\u0000\u0000\u0000"+
		"aZ\u0001\u0000\u0000\u0000a[\u0001\u0000\u0000\u0000a\\\u0001\u0000\u0000"+
		"\u0000a]\u0001\u0000\u0000\u0000a^\u0001\u0000\u0000\u0000a_\u0001\u0000"+
		"\u0000\u0000a`\u0001\u0000\u0000\u0000b\u0005\u0001\u0000\u0000\u0000"+
		"ce\u0003\u0014\n\u0000dc\u0001\u0000\u0000\u0000de\u0001\u0000\u0000\u0000"+
		"ef\u0001\u0000\u0000\u0000fh\u0003\u000e\u0007\u0000gi\u0003\u0016\u000b"+
		"\u0000hg\u0001\u0000\u0000\u0000hi\u0001\u0000\u0000\u0000i\u0007\u0001"+
		"\u0000\u0000\u0000jk\u0003\u0018\f\u0000km\u0005\u0007\u0000\u0000ln\u0003"+
		"\u0010\b\u0000ml\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000no\u0001"+
		"\u0000\u0000\u0000oq\u0003\u000e\u0007\u0000pr\u0003\u0012\t\u0000qp\u0001"+
		"\u0000\u0000\u0000qr\u0001\u0000\u0000\u0000r\t\u0001\u0000\u0000\u0000"+
		"st\u0005\b\u0000\u0000tu\u0003\u000e\u0007\u0000ux\u0005\u0007\u0000\u0000"+
		"vy\u0003\u0016\u000b\u0000wy\u0005\t\u0000\u0000xv\u0001\u0000\u0000\u0000"+
		"xw\u0001\u0000\u0000\u0000yz\u0001\u0000\u0000\u0000zx\u0001\u0000\u0000"+
		"\u0000z{\u0001\u0000\u0000\u0000{\u000b\u0001\u0000\u0000\u0000|}\u0005"+
		"\b\u0000\u0000}~\u0003\u0018\f\u0000~\u007f\u0005\u0007\u0000\u0000\u007f"+
		"\u0080\u0003\u000e\u0007\u0000\u0080\u0083\u0005\u0007\u0000\u0000\u0081"+
		"\u0084\u0003\u0012\t\u0000\u0082\u0084\u0005\t\u0000\u0000\u0083\u0081"+
		"\u0001\u0000\u0000\u0000\u0083\u0082\u0001\u0000\u0000\u0000\u0084\u0085"+
		"\u0001\u0000\u0000\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0085\u0086"+
		"\u0001\u0000\u0000\u0000\u0086\r\u0001\u0000\u0000\u0000\u0087\u0089\u0005"+
		".\u0000\u0000\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000"+
		"\u0000\u0000\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000"+
		"\u0000\u0000\u008b\u000f\u0001\u0000\u0000\u0000\u008c\u008d\u0007\u0000"+
		"\u0000\u0000\u008d\u0011\u0001\u0000\u0000\u0000\u008e\u0093\u0003\u0010"+
		"\b\u0000\u008f\u0093\u0005\'\u0000\u0000\u0090\u0093\u0005*\u0000\u0000"+
		"\u0091\u0093\u0005+\u0000\u0000\u0092\u008e\u0001\u0000\u0000\u0000\u0092"+
		"\u008f\u0001\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0092"+
		"\u0091\u0001\u0000\u0000\u0000\u0093\u0013\u0001\u0000\u0000\u0000\u0094"+
		"\u0095\u0007\u0001\u0000\u0000\u0095\u0015\u0001\u0000\u0000\u0000\u0096"+
		"\u00a5\u0003\u0014\n\u0000\u0097\u00a5\u0005\u001c\u0000\u0000\u0098\u00a5"+
		"\u0005!\u0000\u0000\u0099\u00a5\u0005&\u0000\u0000\u009a\u00a5\u0005\""+
		"\u0000\u0000\u009b\u00a5\u0005(\u0000\u0000\u009c\u00a5\u0005\u001a\u0000"+
		"\u0000\u009d\u00a5\u0005%\u0000\u0000\u009e\u00a5\u0005\u0017\u0000\u0000"+
		"\u009f\u00a5\u0005\u0013\u0000\u0000\u00a0\u00a5\u0005\u0011\u0000\u0000"+
		"\u00a1\u00a5\u0005\u001d\u0000\u0000\u00a2\u00a5\u0005*\u0000\u0000\u00a3"+
		"\u00a5\u0005+\u0000\u0000\u00a4\u0096\u0001\u0000\u0000\u0000\u00a4\u0097"+
		"\u0001\u0000\u0000\u0000\u00a4\u0098\u0001\u0000\u0000\u0000\u00a4\u0099"+
		"\u0001\u0000\u0000\u0000\u00a4\u009a\u0001\u0000\u0000\u0000\u00a4\u009b"+
		"\u0001\u0000\u0000\u0000\u00a4\u009c\u0001\u0000\u0000\u0000\u00a4\u009d"+
		"\u0001\u0000\u0000\u0000\u00a4\u009e\u0001\u0000\u0000\u0000\u00a4\u009f"+
		"\u0001\u0000\u0000\u0000\u00a4\u00a0\u0001\u0000\u0000\u0000\u00a4\u00a1"+
		"\u0001\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a3"+
		"\u0001\u0000\u0000\u0000\u00a5\u0017\u0001\u0000\u0000\u0000\u00a6\u00a7"+
		"\u0007\u0002\u0000\u0000\u00a7\u0019\u0001\u0000\u0000\u0000\u00a8\u00aa"+
		"\u0003\u001e\u000f\u0000\u00a9\u00ab\u0003\u001c\u000e\u0000\u00aa\u00a9"+
		"\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000\u0000\u00ab\u001b"+
		"\u0001\u0000\u0000\u0000\u00ac\u00ae\u0005+\u0000\u0000\u00ad\u00ac\u0001"+
		"\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae\u00af\u0001"+
		"\u0000\u0000\u0000\u00af\u00b0\u0005,\u0000\u0000\u00b0\u001d\u0001\u0000"+
		"\u0000\u0000\u00b1\u00b3\u00032\u0019\u0000\u00b2\u00b4\u00032\u0019\u0000"+
		"\u00b3\u00b2\u0001\u0000\u0000\u0000\u00b3\u00b4\u0001\u0000\u0000\u0000"+
		"\u00b4\u00b6\u0001\u0000\u0000\u0000\u00b5\u00b7\u00032\u0019\u0000\u00b6"+
		"\u00b5\u0001\u0000\u0000\u0000\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7"+
		"\u00bb\u0001\u0000\u0000\u0000\u00b8\u00ba\u0003 \u0010\u0000\u00b9\u00b8"+
		"\u0001\u0000\u0000\u0000\u00ba\u00bd\u0001\u0000\u0000\u0000\u00bb\u00b9"+
		"\u0001\u0000\u0000\u0000\u00bb\u00bc\u0001\u0000\u0000\u0000\u00bc\u001f"+
		"\u0001\u0000\u0000\u0000\u00bd\u00bb\u0001\u0000\u0000\u0000\u00be\u00bf"+
		"\u0005+\u0000\u0000\u00bf\u00c1\u0005.\u0000\u0000\u00c0\u00c2\u0005."+
		"\u0000\u0000\u00c1\u00c0\u0001\u0000\u0000\u0000\u00c1\u00c2\u0001\u0000"+
		"\u0000\u0000\u00c2\u00c4\u0001\u0000\u0000\u0000\u00c3\u00c5\u0005.\u0000"+
		"\u0000\u00c4\u00c3\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001\u0000\u0000"+
		"\u0000\u00c5!\u0001\u0000\u0000\u0000\u00c6\u00c7\u0005\u001d\u0000\u0000"+
		"\u00c7\u00c8\u0005\u0014\u0000\u0000\u00c8\u00c9\u0005\'\u0000\u0000\u00c9"+
		"\u00ca\u0005#\u0000\u0000\u00ca\u00cb\u0005\u0012\u0000\u0000\u00cb\u00cc"+
		"\u0005\u001b\u0000\u0000\u00cc\u00cd\u0005\u0010\u0000\u0000\u00cd\u00ce"+
		"\u0005\u0013\u0000\u0000\u00ce\u00cf\u0005\u0014\u0000\u0000\u00cf\u00d0"+
		"\u0005\u001f\u0000\u0000\u00d0\u00d1\u0005\u0010\u0000\u0000\u00d1\u00d2"+
		"\u0005\u001d\u0000\u0000\u00d2\u00d3\u0005\u0016\u0000\u0000\u00d3\u00d4"+
		"\u0005\u001e\u0000\u0000\u00d4\u00d5\u0005\u001b\u0000\u0000\u00d5\u00d6"+
		"\u0005\u0018\u0000\u0000\u00d6\u00d7\u0005\u001d\u0000\u0000\u00d7\u00d8"+
		"\u0005\u0014\u0000\u0000\u00d8\u00d9\u0005\u0010\u0000\u0000\u00d9\u00da"+
		"\u0005\u0016\u0000\u0000\u00da\u00db\u0005\u0014\u0000\u0000\u00db\u00dc"+
		"\u0005\u0007\u0000\u0000\u00dc#\u0001\u0000\u0000\u0000\u00dd\u00de\u0003"+
		"\"\u0011\u0000\u00de\u00df\u0003\u001a\r\u0000\u00df%\u0001\u0000\u0000"+
		"\u0000\u00e0\u00e2\u00032\u0019\u0000\u00e1\u00e3\u00032\u0019\u0000\u00e2"+
		"\u00e1\u0001\u0000\u0000\u0000\u00e2\u00e3\u0001\u0000\u0000\u0000\u00e3"+
		"\'\u0001\u0000\u0000\u0000\u00e4\u00e5\u0005\u0016\u0000\u0000\u00e5\u00e6"+
		"\u0005\u0018\u0000\u0000\u00e6\u00e7\u0005\"\u0000\u0000\u00e7\u00e8\u0005"+
		"\u0010\u0000\u0000\u00e8\u00e9\u0005\u0018\u0000\u0000\u00e9\u00ea\u0005"+
		"\u0013\u0000\u0000\u00ea\u00eb\u0005\u0007\u0000\u0000\u00eb)\u0001\u0000"+
		"\u0000\u0000\u00ec\u00ed\u0003(\u0014\u0000\u00ed\u00ee\u0003&\u0013\u0000"+
		"\u00ee+\u0001\u0000\u0000\u0000\u00ef\u00f0\u0005.\u0000\u0000\u00f0\u00f1"+
		"\u0005.\u0000\u0000\u00f1\u00f4\u00032\u0019\u0000\u00f2\u00f4\u0005\n"+
		"\u0000\u0000\u00f3\u00ef\u0001\u0000\u0000\u0000\u00f3\u00f2\u0001\u0000"+
		"\u0000\u0000\u00f4-\u0001\u0000\u0000\u0000\u00f5\u00f6\u0005\u001d\u0000"+
		"\u0000\u00f6\u00f7\u0005\u0014\u0000\u0000\u00f7\u00f8\u0005\'\u0000\u0000"+
		"\u00f8\u00f9\u0005#\u0000\u0000\u00f9\u00fa\u0005\"\u0000\u0000\u00fa"+
		"\u00fb\u0005#\u0000\u0000\u00fb\u00fc\u0005!\u0000\u0000\u00fc\u00fd\u0005"+
		"\u0010\u0000\u0000\u00fd\u00fe\u0005\u0018\u0000\u0000\u00fe\u00ff\u0005"+
		"\u001d\u0000\u0000\u00ff\u0100\u0005\u0012\u0000\u0000\u0100\u0101\u0005"+
		"\u001b\u0000\u0000\u0101\u0102\u0005\u0010\u0000\u0000\u0102\u0103\u0005"+
		"\u0013\u0000\u0000\u0103\u0104\u0005\u0014\u0000\u0000\u0104\u0105\u0005"+
		"\u0007\u0000\u0000\u0105/\u0001\u0000\u0000\u0000\u0106\u0107\u0003.\u0017"+
		"\u0000\u0107\u0108\u0003,\u0016\u0000\u01081\u0001\u0000\u0000\u0000\u0109"+
		"\u010a\u0007\u0003\u0000\u0000\u010a3\u0001\u0000\u0000\u0000\u010b\u010d"+
		"\u0005\u000b\u0000\u0000\u010c\u010e\u00036\u001b\u0000\u010d\u010c\u0001"+
		"\u0000\u0000\u0000\u010d\u010e\u0001\u0000\u0000\u0000\u010e\u010f\u0001"+
		"\u0000\u0000\u0000\u010f\u0110\u00038\u001c\u0000\u0110\u0111\u0005\f"+
		"\u0000\u0000\u0111\u0112\u0003:\u001d\u0000\u0112\u0113\u0005\r\u0000"+
		"\u0000\u01135\u0001\u0000\u0000\u0000\u0114\u0115\u0005\u000e\u0000\u0000"+
		"\u01157\u0001\u0000\u0000\u0000\u0116\u0118\u0005.\u0000\u0000\u0117\u0116"+
		"\u0001\u0000\u0000\u0000\u0118\u0119\u0001\u0000\u0000\u0000\u0119\u0117"+
		"\u0001\u0000\u0000\u0000\u0119\u011a\u0001\u0000\u0000\u0000\u011a9\u0001"+
		"\u0000\u0000\u0000\u011b\u0120\u0003\u0002\u0001\u0000\u011c\u011d\u0005"+
		"\u000f\u0000\u0000\u011d\u011f\u0003\u0002\u0001\u0000\u011e\u011c\u0001"+
		"\u0000\u0000\u0000\u011f\u0122\u0001\u0000\u0000\u0000\u0120\u011e\u0001"+
		"\u0000\u0000\u0000\u0120\u0121\u0001\u0000\u0000\u0000\u0121;\u0001\u0000"+
		"\u0000\u0000\u0122\u0120\u0001\u0000\u0000\u0000\u001bKSUadhmqxz\u0083"+
		"\u0085\u008a\u0092\u00a4\u00aa\u00ad\u00b3\u00b6\u00bb\u00c1\u00c4\u00e2"+
		"\u00f3\u010d\u0119\u0120";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
