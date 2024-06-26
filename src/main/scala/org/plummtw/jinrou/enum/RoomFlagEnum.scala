package org.plummtw.jinrou.enum

object RoomFlagEnum extends Enumeration {
  type RoomFlagEnum = Value
  
  // Game Option
  val TEST_MODE         = Value("TM")
  val ANONYMOUS_MODE    = Value("AM")
  val WISH_ROLE         = Value("WR")
  val NO_DUMMY          = Value("ND")
  val DUMMY_REVEAL      = Value("DR")
  val VOTE_REVEAL       = Value("VR")
  val DEATH_LOOK        = Value("DL")
  val GEMINI_TALK       = Value("GT")
  val AUTO_VOTE         = Value("AV")
  val WEATHER           = Value("WE")
  val WEATHER1          = Value("WF")
  val ITEM_MODE         = Value("IT")
  val ITEM_CUBIC        = Value("IC")
  val CUBIC_CHANNEL     = Value("CC")
  val CUBIC_INIT        = Value("CI")
  val CUBIC_IMMEDIATE   = Value("CM")
  val LOVER_INIT        = Value("LI")
  val LOVER_LETTER_EXCHANGE     = Value("LE")
  val MOB_MODE          = Value("MM")
  val MOB_MODE1         = Value("MN")
  
  // Optional Role

  val ROLE_CLERIC    = Value("RC")
  val ROLE_HERBALIST = Value("RL")
  val ROLE_POISONER  = Value("RP")
  val ROLE_RUNNER    = Value("RR")
  val ROLE_AUGHUNTER = Value("RE")
  val ROLE_SCHOLAR   = Value("RO")
  val ROLE_ARCHMAGE  = Value("RZ")

  val ROLE_WOLFCUB   = Value("RX")
  val ROLE_SORCEROR  = Value("RU")
  
  val ROLE_BETRAYER  = Value("RB")
  val ROLE_GODFAT    = Value("RT")
  
  val ROLE_DEMON     = Value("RD")
  val ROLE_FALLEN_ANGEL  = Value("Rf")
  val ROLE_PONTIFF   = Value("RJ")
  val ROLE_PENGUIN   = Value("RK")

  val ROLE_INHERITER = Value("RI")  
  val ROLE_SHIFTER   = Value("RS")

  val ROLE_HERMIT     = Value("RY")
  val ROLE_CARDMASTER = Value("RQ")
  
  val SUBROLE_MEMORYLOSS4  = Value("S4")
  val SUBROLE_MEMORYLOSS4_2  = Value("S_4")
  val SUBROLE_MEMORYLOSS6  = Value("S6")
  val SUBROLE_MEMORYLOSS8  = Value("S8")
  val SUBROLE_FAKEAUGURER  = Value("SF")
  val SUBROLE_SUDDENDEATH  = Value("SS")
  val SUBROLE_AVENGER        = Value("SV")
  val SUBROLE_WOLFBELIEVER = Value("SW")
  val SUBROLE_ALPHAWOLF      = Value("SX")
  val SUBROLE_WISEWOLF         = Value("SY")
  val SUBROLE_WOLFSTAMP      = Value("SZ")
  val SUBROLE_SUBPONTIFF     = Value("SP")
  val SUBROLE_HASHIHIME      = Value("SH")
  val SUBROLE_PLUS           = Value("S+")
  
  // Role Adjustment
  val VILLAGER_DETECT  = Value("V1")
  val NECROMANCER_OPTION1 = Value("N1")
  val GEMINI_DAYTALK    = Value("G1")
  val GEMINI_BALANCE    = Value("G2")
  val GEMINI_LEGACY     = Value("G3")
  val HUNTER_OPTION1    = Value("H1")
  val HUNTER_OPTION2     = Value("H2")
  val CLERIC_OPTION1    = Value("C1")
  val CLERIC_OPTION2    = Value("C2")
  val CLERIC_OPTION3    = Value("C3")
  val CLERIC_OPTION4    = Value("C4")
  val HERBALIST_MIX      = Value("L1")
  val HERBALIST_DROP     = Value("L2")
  val SCHOLAR_OPTION1    = Value("O1")
  val SCHOLAR_OPTION2    = Value("O2")
  val SCHOLAR_OPTION3    = Value("O3")
  val SCHOLAR_OPTION4    = Value("O4")
  val WEREWOLF_OPTION1  = Value("W1")
  val WOLFCUB_OPTION1  = Value("X1")
  val MADMAN_KNOWLEDGE = Value("M1")
  val MADMAN_SUICIDE   = Value("M2")
  val MADMAN_STUN      = Value("M3")
  val MADMAN_DUEL      = Value("M4")
  val SORCEROR_BELIEVE = Value("U1")
  val SORCEROR_WHISPER1 = Value("U2")
  val SORCEROR_SHOUT1    = Value("U3")
  val SORCEROR_SEAR      = Value("U4")
  val SORCEROR_SUMMON    = Value("U5")
  val RUNNER_OPTION1   = Value("R1")
  val RUNNER_OPTION2   = Value("R2")
  val RUNNER_OPTION3   = Value("R3")
  val RUNNER_OPTION4   = Value("R4")
  val FOX_OPTION1       = Value("F1")
  val FOX_OPTION2       = Value("F2")
  val FOX_OPTION3       = Value("F3")
  val FOX_OPTION4       = Value("F4")
  val BETRAYER_OPTION1  = Value("B1")
  val BETRAYER_OPTION2   = Value("B2")
  val BETRAYER_OPTION3   = Value("B3")
  val GODFAT_SPECIAL1  = Value("T1")
  val GODFAT_SPECIAL2   = Value("T2")
  val GODFAT_SPECIAL3   = Value("T3")
  val GODFAT_SPECIAL4   = Value("T4")
  val DEMON_OPTION1    = Value("D1")
  val DEMON_OPTION2    = Value("D2")
  val DEMON_OPTION3    = Value("D3")
  val PENGUIN_OPTION1    = Value("K1")
  val PENGUIN_OPTION2    = Value("K2")
  val PENGUIN_OPTION3    = Value("K3")
  val PENGUIN_OPTION4    = Value("K4")
  val PONTIFF_OPTION1  = Value("J1")
  val PONTIFF_OPTION2    = Value("J2")
  val PONTIFF_OPTION3    = Value("J3")
  val PONTIFF_OPTION4    = Value("J4")
  val INHERITER_REVEAL = Value("I1")
  val INHERITER_NEUTRAL = Value("I2")
  val SHIFTER_REVEAL   = Value("S1")
  val SHIFTER_LINKS    = Value("S2")
  val SHIFTER_RANDOM = Value("S3")
  val HERETIC_OPTION1 = Value("#1")
  val FALLENANGEL_OPTION1  = Value("f1")
  val ARCHMAGE_OPTION1   = Value("Z1")
  val CARDMASTER_OPTION1 = Value("Q1")
  val GM_PENGUIN1        = Value("(1")
  val GM_HERMIT1          = Value("(2")
  val GM_PONTIFF1         = Value("(3")
  val GM_HERETIC1         = Value("(4")
  val GM_HERETIC2         = Value("(5")
  val GM_SPY1         = Value("(6")
  
  val FLAGNAME_MAP   = Map(
    TEST_MODE    -> "(測)",
	ANONYMOUS_MODE -> "(匿)",
    WISH_ROLE    -> "(希)",
    NO_DUMMY     -> "(無替)",
    DUMMY_REVEAL -> "(替)",
    VOTE_REVEAL  -> "(票)",
    DEATH_LOOK   -> "(靈)",
    GEMINI_TALK  -> "(共)",
    AUTO_VOTE    -> "(自投)",
    WEATHER      -> "(天候)",
    WEATHER1     -> "(天候1)",
    ITEM_MODE    -> "(道具)",
    MOB_MODE     -> "(暴)",
    MOB_MODE1    -> "(暴1)",
    ITEM_CUBIC   -> "(箭)",
    CUBIC_CHANNEL-> "(戀1)",
    CUBIC_INIT   -> "(戀2)",
    CUBIC_IMMEDIATE -> "(戀3)",
	LOVER_INIT -> "(戀4)",
	LOVER_LETTER_EXCHANGE -> "(戀5)",
  
    // Optional Role

    ROLE_CLERIC    -> "[牧]",
    ROLE_HERBALIST -> "[藥]",
    ROLE_POISONER  -> "[毒]",
    ROLE_RUNNER    -> "[逃]",
    ROLE_SCHOLAR   -> "[學]",

    ROLE_SORCEROR  -> "[巫]",
    ROLE_WOLFCUB   -> "[幼]",
  
    ROLE_BETRAYER  -> "[背]",
    ROLE_GODFAT    -> "[哥]",
  
    ROLE_DEMON     -> "[惡]",
    ROLE_PONTIFF   -> "[教]",

    ROLE_INHERITER -> "[繼]",
    ROLE_SHIFTER   -> "[模]",
	ROLE_ARCHMAGE  -> "[大]",
	ROLE_AUGHUNTER -> "[占獵]",
    ROLE_CARDMASTER -> "[卡]",
	ROLE_HERMIT    -> "[隱]",
	ROLE_PENGUIN   -> "[企]",
	ROLE_FALLEN_ANGEL -> "[墮]",

    SUBROLE_MEMORYLOSS4  -> "[忘4]",
    SUBROLE_MEMORYLOSS4_2-> "[忘4+]",
    SUBROLE_MEMORYLOSS6  -> "[忘6]",
    SUBROLE_MEMORYLOSS8  -> "[忘8]",
    SUBROLE_FAKEAUGURER  -> "[冒]",
    SUBROLE_SUDDENDEATH  -> "[絕]",
    SUBROLE_AVENGER      -> "[復]",
    SUBROLE_WOLFBELIEVER  -> "[狼信]",
    SUBROLE_ALPHAWOLF     -> "[大狼]",
    SUBROLE_WISEWOLF      -> "[智狼]",
    SUBROLE_WOLFSTAMP     -> "[狼印]",
    SUBROLE_SUBPONTIFF    -> "[副&無]",
    SUBROLE_HASHIHIME     -> "[橋]",
    SUBROLE_PLUS          -> "[副+]",
  
    // Role Adjustment
    VILLAGER_DETECT  -> "<村>",
    NECROMANCER_OPTION1 -> "<靈>",
    GEMINI_DAYTALK   -> "<共1>",
    GEMINI_BALANCE   -> "<共2>",
    GEMINI_LEGACY    -> "<共3>",
    HUNTER_OPTION1   -> "<獵1>",
    HUNTER_OPTION2   -> "<獵2>",
    CLERIC_OPTION1   -> "<牧1>",
    CLERIC_OPTION2   -> "<牧2>",
	CLERIC_OPTION3   -> "<牧3>",
	CLERIC_OPTION4   -> "<牧4>",
    HERBALIST_MIX    -> "<藥1>",
    HERBALIST_DROP   -> "<藥2>",
    SCHOLAR_OPTION1  -> "<學1>",
    SCHOLAR_OPTION2  -> "<學2>",
    SCHOLAR_OPTION3  -> "<學3>",
    SCHOLAR_OPTION4  -> "<學4>",
    WEREWOLF_OPTION1  -> "<狼>",
    WOLFCUB_OPTION1  -> "<幼>",
    MADMAN_KNOWLEDGE -> "<狂1>",
    MADMAN_SUICIDE   -> "<狂2>",
    MADMAN_STUN      -> "<狂3>",
    MADMAN_DUEL      -> "<狂4>",
    SORCEROR_BELIEVE -> "<巫1>",
    SORCEROR_WHISPER1 -> "<巫2>",
    SORCEROR_SHOUT1   -> "<巫3>",
    SORCEROR_SEAR     -> "<巫4>",
    SORCEROR_SUMMON   -> "<巫5>",
    RUNNER_OPTION1   -> "<逃1>",
    RUNNER_OPTION2   -> "<逃2>",
    RUNNER_OPTION3   -> "<逃3>",
    RUNNER_OPTION4   -> "<逃4>",
    FOX_OPTION1      -> "<狐1>",
    FOX_OPTION2      -> "<狐2>",
    FOX_OPTION3      -> "<狐3>",
    FOX_OPTION4      -> "<狐4>",
    BETRAYER_OPTION1 -> "<背1>",
    BETRAYER_OPTION2 -> "<背2>",
    BETRAYER_OPTION3 -> "<背3>",
    GODFAT_SPECIAL1  -> "<哥1>",
    GODFAT_SPECIAL2  -> "<哥2>",
    GODFAT_SPECIAL3  -> "<哥3>",
    GODFAT_SPECIAL4  -> "<哥4>",
    DEMON_OPTION1    -> "<惡1>",
    DEMON_OPTION2    -> "<惡2>",
    DEMON_OPTION3    -> "<惡3>",
    PENGUIN_OPTION1  -> "<企1>",
    PENGUIN_OPTION2  -> "<企2>",
    PENGUIN_OPTION3  -> "<企3>",
	PENGUIN_OPTION4  -> "<企4>",
    PONTIFF_OPTION1  -> "<教1>",
    PONTIFF_OPTION2  -> "<教2>",
    PONTIFF_OPTION3  -> "<教3>",
	PONTIFF_OPTION4  -> "<教4>",
    INHERITER_REVEAL -> "<繼1>",
    INHERITER_NEUTRAL -> "<繼2>",
    SHIFTER_REVEAL   -> "<模1>",
    SHIFTER_LINKS    -> "<模2>",
	SHIFTER_RANDOM -> "<模3>",
	HERETIC_OPTION1    -> "<魔1>",
	FALLENANGEL_OPTION1    -> "<墮1>",

    ARCHMAGE_OPTION1   -> "【大初】",
    CARDMASTER_OPTION1 -> "【卡初】",

    
    GM_PENGUIN1        -> "【企初】",
    GM_HERMIT1         -> "【隱初】",
    GM_PONTIFF1        -> "【教替】",
	GM_HERETIC1        -> "【端村】",
	GM_HERETIC2        -> "【端繼】",
	GM_SPY1            -> "【諜狂】"
  )
  
  def flag_name(flag : RoomFlagEnum.Value) = {
    FLAGNAME_MAP.get(flag)
  }
}