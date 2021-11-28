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
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, A=7, B=8, C=9, D=10, E=11, 
		F=12, G=13, H=14, I=15, J=16, K=17, L=18, M=19, N=20, O=21, P=22, Q=23, 
		R=24, S=25, T=26, U=27, V=28, W=29, X=30, Y=31, Z=32, MINUS=33, DOT=34, 
		ASTERISK=35, ORF=36, NUMBER=37, WHITESPACE=38;
	public static final int
		RULE_expr = 0, RULE_single = 1, RULE_nuc_mut = 2, RULE_aa_mut = 3, RULE_position = 4, 
		RULE_aa = 5, RULE_aa_mutated = 6, RULE_nuc = 7, RULE_nuc_mutated = 8, 
		RULE_gene = 9, RULE_pango_query = 10, RULE_pango_include_sub = 11, RULE_pango_lineage = 12, 
		RULE_pango_number_component = 13, RULE_gisaid_clade = 14, RULE_nextstrain_clade = 15, 
		RULE_character = 16;
	private static String[] makeRuleNames() {
		return new String[] {
			"expr", "single", "nuc_mut", "aa_mut", "position", "aa", "aa_mutated", 
			"nuc", "nuc_mutated", "gene", "pango_query", "pango_include_sub", "pango_lineage", 
			"pango_number_component", "gisaid_clade", "nextstrain_clade", "character"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'!'", "'&'", "'|'", "'('", "')'", "':'", "'A'", "'B'", "'C'", 
			"'D'", "'E'", "'F'", "'G'", "'H'", "'I'", "'J'", "'K'", "'L'", "'M'", 
			"'N'", "'O'", "'P'", "'Q'", "'R'", "'S'", "'T'", "'U'", "'V'", "'W'", 
			"'X'", "'Y'", "'Z'", "'-'", "'.'", "'*'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, "A", "B", "C", "D", "E", "F", 
			"G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", 
			"U", "V", "W", "X", "Y", "Z", "MINUS", "DOT", "ASTERISK", "ORF", "NUMBER", 
			"WHITESPACE"
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

	public static class ExprContext extends ParserRuleContext {
		public SingleContext single() {
			return getRuleContext(SingleContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VariantQueryListener ) ((VariantQueryListener)listener).exitExpr(this);
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
		int _startState = 0;
		enterRecursionRule(_localctx, 0, RULE_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
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
				setState(35);
				single();
				}
				break;
			case T__0:
				{
				setState(36);
				match(T__0);
				setState(37);
				expr(4);
				}
				break;
			case T__3:
				{
				setState(38);
				match(T__3);
				setState(39);
				expr(0);
				setState(40);
				match(T__4);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(52);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(50);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(44);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(45);
						match(T__1);
						setState(46);
						expr(4);
						}
						break;
					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(47);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(48);
						match(T__2);
						setState(49);
						expr(3);
						}
						break;
					}
					} 
				}
				setState(54);
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
		public Pango_queryContext pango_query() {
			return getRuleContext(Pango_queryContext.class,0);
		}
		public Gisaid_cladeContext gisaid_clade() {
			return getRuleContext(Gisaid_cladeContext.class,0);
		}
		public Nextstrain_cladeContext nextstrain_clade() {
			return getRuleContext(Nextstrain_cladeContext.class,0);
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
		enterRule(_localctx, 2, RULE_single);
		try {
			setState(60);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(55);
				aa_mut();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(56);
				nuc_mut();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(57);
				pango_query();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(58);
				gisaid_clade();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(59);
				nextstrain_clade();
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
		enterRule(_localctx, 4, RULE_nuc_mut);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(63);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << C) | (1L << G) | (1L << T))) != 0)) {
				{
				setState(62);
				nuc();
				}
			}

			setState(65);
			position();
			setState(67);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				setState(66);
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
		enterRule(_localctx, 6, RULE_aa_mut);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			gene();
			setState(70);
			match(T__5);
			setState(72);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << A) | (1L << C) | (1L << D) | (1L << E) | (1L << F) | (1L << G) | (1L << H) | (1L << I) | (1L << K) | (1L << L) | (1L << M) | (1L << N) | (1L << P) | (1L << Q) | (1L << R) | (1L << S) | (1L << T) | (1L << V) | (1L << W) | (1L << Y) | (1L << ASTERISK))) != 0)) {
				{
				setState(71);
				aa();
				}
			}

			setState(74);
			position();
			setState(76);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				{
				setState(75);
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
		enterRule(_localctx, 8, RULE_position);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(79); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(78);
					match(NUMBER);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(81); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
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
		enterRule(_localctx, 10, RULE_aa);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(83);
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
		enterRule(_localctx, 12, RULE_aa_mutated);
		try {
			setState(88);
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
				setState(85);
				aa();
				}
				break;
			case X:
				enterOuterAlt(_localctx, 2);
				{
				setState(86);
				match(X);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 3);
				{
				setState(87);
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
		enterRule(_localctx, 14, RULE_nuc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
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
		enterRule(_localctx, 16, RULE_nuc_mutated);
		try {
			setState(105);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case A:
			case C:
			case G:
			case T:
				enterOuterAlt(_localctx, 1);
				{
				setState(92);
				nuc();
				}
				break;
			case M:
				enterOuterAlt(_localctx, 2);
				{
				setState(93);
				match(M);
				}
				break;
			case R:
				enterOuterAlt(_localctx, 3);
				{
				setState(94);
				match(R);
				}
				break;
			case W:
				enterOuterAlt(_localctx, 4);
				{
				setState(95);
				match(W);
				}
				break;
			case S:
				enterOuterAlt(_localctx, 5);
				{
				setState(96);
				match(S);
				}
				break;
			case Y:
				enterOuterAlt(_localctx, 6);
				{
				setState(97);
				match(Y);
				}
				break;
			case K:
				enterOuterAlt(_localctx, 7);
				{
				setState(98);
				match(K);
				}
				break;
			case V:
				enterOuterAlt(_localctx, 8);
				{
				setState(99);
				match(V);
				}
				break;
			case H:
				enterOuterAlt(_localctx, 9);
				{
				setState(100);
				match(H);
				}
				break;
			case D:
				enterOuterAlt(_localctx, 10);
				{
				setState(101);
				match(D);
				}
				break;
			case B:
				enterOuterAlt(_localctx, 11);
				{
				setState(102);
				match(B);
				}
				break;
			case N:
				enterOuterAlt(_localctx, 12);
				{
				setState(103);
				match(N);
				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 13);
				{
				setState(104);
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
		enterRule(_localctx, 18, RULE_gene);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(107);
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
		enterRule(_localctx, 20, RULE_pango_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			pango_lineage();
			setState(111);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(110);
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
		enterRule(_localctx, 22, RULE_pango_include_sub);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(113);
				match(DOT);
				}
			}

			setState(116);
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
		enterRule(_localctx, 24, RULE_pango_lineage);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			character();
			setState(120);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				{
				setState(119);
				character();
				}
				break;
			}
			setState(123);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				{
				setState(122);
				pango_number_component();
				}
				break;
			}
			setState(126);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(125);
				pango_number_component();
				}
				break;
			}
			setState(129);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				setState(128);
				pango_number_component();
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
		enterRule(_localctx, 26, RULE_pango_number_component);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(131);
			match(DOT);
			setState(132);
			match(NUMBER);
			setState(134);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(133);
				match(NUMBER);
				}
				break;
			}
			setState(137);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				{
				setState(136);
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

	public static class Gisaid_cladeContext extends ParserRuleContext {
		public TerminalNode G() { return getToken(VariantQueryParser.G, 0); }
		public List<TerminalNode> I() { return getTokens(VariantQueryParser.I); }
		public TerminalNode I(int i) {
			return getToken(VariantQueryParser.I, i);
		}
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode A() { return getToken(VariantQueryParser.A, 0); }
		public TerminalNode D() { return getToken(VariantQueryParser.D, 0); }
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
		enterRule(_localctx, 28, RULE_gisaid_clade);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(139);
			match(G);
			setState(140);
			match(I);
			setState(141);
			match(S);
			setState(142);
			match(A);
			setState(143);
			match(I);
			setState(144);
			match(D);
			setState(145);
			match(T__5);
			setState(146);
			character();
			setState(148);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				{
				setState(147);
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

	public static class Nextstrain_cladeContext extends ParserRuleContext {
		public List<TerminalNode> N() { return getTokens(VariantQueryParser.N); }
		public TerminalNode N(int i) {
			return getToken(VariantQueryParser.N, i);
		}
		public TerminalNode E() { return getToken(VariantQueryParser.E, 0); }
		public TerminalNode X() { return getToken(VariantQueryParser.X, 0); }
		public List<TerminalNode> T() { return getTokens(VariantQueryParser.T); }
		public TerminalNode T(int i) {
			return getToken(VariantQueryParser.T, i);
		}
		public TerminalNode S() { return getToken(VariantQueryParser.S, 0); }
		public TerminalNode R() { return getToken(VariantQueryParser.R, 0); }
		public TerminalNode A() { return getToken(VariantQueryParser.A, 0); }
		public TerminalNode I() { return getToken(VariantQueryParser.I, 0); }
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
		enterRule(_localctx, 30, RULE_nextstrain_clade);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(150);
			match(N);
			setState(151);
			match(E);
			setState(152);
			match(X);
			setState(153);
			match(T);
			setState(154);
			match(S);
			setState(155);
			match(T);
			setState(156);
			match(R);
			setState(157);
			match(A);
			setState(158);
			match(I);
			setState(159);
			match(N);
			setState(160);
			match(T__5);
			setState(161);
			match(NUMBER);
			setState(162);
			match(NUMBER);
			setState(163);
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
		enterRule(_localctx, 32, RULE_character);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 0:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3(\u00aa\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\5\2-\n\2\3\2\3\2\3\2\3\2\3\2\3\2\7\2"+
		"\65\n\2\f\2\16\28\13\2\3\3\3\3\3\3\3\3\3\3\5\3?\n\3\3\4\5\4B\n\4\3\4\3"+
		"\4\5\4F\n\4\3\5\3\5\3\5\5\5K\n\5\3\5\3\5\5\5O\n\5\3\6\6\6R\n\6\r\6\16"+
		"\6S\3\7\3\7\3\b\3\b\3\b\5\b[\n\b\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3"+
		"\n\3\n\3\n\3\n\3\n\3\n\5\nl\n\n\3\13\3\13\3\f\3\f\5\fr\n\f\3\r\5\ru\n"+
		"\r\3\r\3\r\3\16\3\16\5\16{\n\16\3\16\5\16~\n\16\3\16\5\16\u0081\n\16\3"+
		"\16\5\16\u0084\n\16\3\17\3\17\3\17\5\17\u0089\n\17\3\17\5\17\u008c\n\17"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\5\20\u0097\n\20\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22"+
		"\3\22\3\22\2\3\2\23\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"\2\6\t\2"+
		"\t\t\13\21\23\26\30\34\36\37!!%%\6\2\t\t\13\13\17\17\34\34\6\2\r\r\25"+
		"\26\33\33&&\3\2\t\"\2\u00bc\2,\3\2\2\2\4>\3\2\2\2\6A\3\2\2\2\bG\3\2\2"+
		"\2\nQ\3\2\2\2\fU\3\2\2\2\16Z\3\2\2\2\20\\\3\2\2\2\22k\3\2\2\2\24m\3\2"+
		"\2\2\26o\3\2\2\2\30t\3\2\2\2\32x\3\2\2\2\34\u0085\3\2\2\2\36\u008d\3\2"+
		"\2\2 \u0098\3\2\2\2\"\u00a7\3\2\2\2$%\b\2\1\2%-\5\4\3\2&\'\7\3\2\2\'-"+
		"\5\2\2\6()\7\6\2\2)*\5\2\2\2*+\7\7\2\2+-\3\2\2\2,$\3\2\2\2,&\3\2\2\2,"+
		"(\3\2\2\2-\66\3\2\2\2./\f\5\2\2/\60\7\4\2\2\60\65\5\2\2\6\61\62\f\4\2"+
		"\2\62\63\7\5\2\2\63\65\5\2\2\5\64.\3\2\2\2\64\61\3\2\2\2\658\3\2\2\2\66"+
		"\64\3\2\2\2\66\67\3\2\2\2\67\3\3\2\2\28\66\3\2\2\29?\5\b\5\2:?\5\6\4\2"+
		";?\5\26\f\2<?\5\36\20\2=?\5 \21\2>9\3\2\2\2>:\3\2\2\2>;\3\2\2\2><\3\2"+
		"\2\2>=\3\2\2\2?\5\3\2\2\2@B\5\20\t\2A@\3\2\2\2AB\3\2\2\2BC\3\2\2\2CE\5"+
		"\n\6\2DF\5\22\n\2ED\3\2\2\2EF\3\2\2\2F\7\3\2\2\2GH\5\24\13\2HJ\7\b\2\2"+
		"IK\5\f\7\2JI\3\2\2\2JK\3\2\2\2KL\3\2\2\2LN\5\n\6\2MO\5\16\b\2NM\3\2\2"+
		"\2NO\3\2\2\2O\t\3\2\2\2PR\7\'\2\2QP\3\2\2\2RS\3\2\2\2SQ\3\2\2\2ST\3\2"+
		"\2\2T\13\3\2\2\2UV\t\2\2\2V\r\3\2\2\2W[\5\f\7\2X[\7 \2\2Y[\7#\2\2ZW\3"+
		"\2\2\2ZX\3\2\2\2ZY\3\2\2\2[\17\3\2\2\2\\]\t\3\2\2]\21\3\2\2\2^l\5\20\t"+
		"\2_l\7\25\2\2`l\7\32\2\2al\7\37\2\2bl\7\33\2\2cl\7!\2\2dl\7\23\2\2el\7"+
		"\36\2\2fl\7\20\2\2gl\7\f\2\2hl\7\n\2\2il\7\26\2\2jl\7#\2\2k^\3\2\2\2k"+
		"_\3\2\2\2k`\3\2\2\2ka\3\2\2\2kb\3\2\2\2kc\3\2\2\2kd\3\2\2\2ke\3\2\2\2"+
		"kf\3\2\2\2kg\3\2\2\2kh\3\2\2\2ki\3\2\2\2kj\3\2\2\2l\23\3\2\2\2mn\t\4\2"+
		"\2n\25\3\2\2\2oq\5\32\16\2pr\5\30\r\2qp\3\2\2\2qr\3\2\2\2r\27\3\2\2\2"+
		"su\7$\2\2ts\3\2\2\2tu\3\2\2\2uv\3\2\2\2vw\7%\2\2w\31\3\2\2\2xz\5\"\22"+
		"\2y{\5\"\22\2zy\3\2\2\2z{\3\2\2\2{}\3\2\2\2|~\5\34\17\2}|\3\2\2\2}~\3"+
		"\2\2\2~\u0080\3\2\2\2\177\u0081\5\34\17\2\u0080\177\3\2\2\2\u0080\u0081"+
		"\3\2\2\2\u0081\u0083\3\2\2\2\u0082\u0084\5\34\17\2\u0083\u0082\3\2\2\2"+
		"\u0083\u0084\3\2\2\2\u0084\33\3\2\2\2\u0085\u0086\7$\2\2\u0086\u0088\7"+
		"\'\2\2\u0087\u0089\7\'\2\2\u0088\u0087\3\2\2\2\u0088\u0089\3\2\2\2\u0089"+
		"\u008b\3\2\2\2\u008a\u008c\7\'\2\2\u008b\u008a\3\2\2\2\u008b\u008c\3\2"+
		"\2\2\u008c\35\3\2\2\2\u008d\u008e\7\17\2\2\u008e\u008f\7\21\2\2\u008f"+
		"\u0090\7\33\2\2\u0090\u0091\7\t\2\2\u0091\u0092\7\21\2\2\u0092\u0093\7"+
		"\f\2\2\u0093\u0094\7\b\2\2\u0094\u0096\5\"\22\2\u0095\u0097\5\"\22\2\u0096"+
		"\u0095\3\2\2\2\u0096\u0097\3\2\2\2\u0097\37\3\2\2\2\u0098\u0099\7\26\2"+
		"\2\u0099\u009a\7\r\2\2\u009a\u009b\7 \2\2\u009b\u009c\7\34\2\2\u009c\u009d"+
		"\7\33\2\2\u009d\u009e\7\34\2\2\u009e\u009f\7\32\2\2\u009f\u00a0\7\t\2"+
		"\2\u00a0\u00a1\7\21\2\2\u00a1\u00a2\7\26\2\2\u00a2\u00a3\7\b\2\2\u00a3"+
		"\u00a4\7\'\2\2\u00a4\u00a5\7\'\2\2\u00a5\u00a6\5\"\22\2\u00a6!\3\2\2\2"+
		"\u00a7\u00a8\t\5\2\2\u00a8#\3\2\2\2\26,\64\66>AEJNSZkqtz}\u0080\u0083"+
		"\u0088\u008b\u0096";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}