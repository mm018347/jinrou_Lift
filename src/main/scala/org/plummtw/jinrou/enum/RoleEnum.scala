package org.plummtw.jinrou.enum

import org.plummtw.jinrou.data._

object RoleEnum extends Enumeration {
  type RoleEnum = Value
  
  val NONE        = Value("0")
  
  // VILLAGER SIDE
  val VILLAGER    = Value("V")
  val HERMIT      = Value("Y")
  val AUGURER     = Value("A")
  val NECROMANCER = Value("N")
  val HUNTER      = Value("H")
  val GEMINI      = Value("G")
  
  val CLERIC      = Value("C")
  val HERBALIST   = Value("L")
  val ALCHEMIST   = Value("l")
  val POISONER    = Value("P")
  val RUNNER      = Value("R")
  
  val AUGHUNTER   = Value("E")
  val SCHOLAR     = Value("O")
  val ARCHMAGE    = Value("Z")
  val CARDMASTER   = Value("Q")
  
  // WEREWOLF SIDE
  val WEREWOLF    = Value("W")
  val WOLFCUB     = Value("X")
  val MADMAN      = Value("M")
  val SORCEROR    = Value("U")
  val SPY         = Value("y")
  
  // FOX SIDE
  val FOX         = Value("F")
  val BETRAYER    = Value("B")
  val GODFAT      = Value("T")
  
  // DEMON SIDE
  val DEMON       = Value("D")
  val FALLEN_ANGEL = Value("f")
  val HERETIC = Value("h")
  
  // PENGUIN SIDE
  val PENGUIN     = Value("K")

  val PONTIFF     = Value("J")
  
  // NON-SPECIFIC-SIDE
  val INHERITER   = Value("I")
  val SHIFTER     = Value("S")
  
  
  val FAIRY = Value("a")
  val VAMPIRE    = Value("m")
  
  
  def ROLE_MAP   = scala.collection.immutable.TreeMap(
     NONE        -> RoleNone,
  
     VILLAGER    -> RoleVillager,
     HERMIT      -> RoleHermit,
     AUGURER     -> RoleAugurer,
     NECROMANCER -> RoleNecromancer,
     HUNTER      -> RoleHunter,
     GEMINI      -> RoleGemini,
     
     CLERIC      -> RoleCleric,
     HERBALIST   -> RoleHerbalist,
     ALCHEMIST   -> RoleAlchemist,
     POISONER    -> RolePoisoner,
     RUNNER      -> RoleRunner,
     
     WEREWOLF    -> RoleWerewolf,
     WOLFCUB     -> RoleWolfcub,
     MADMAN      -> RoleMadman,
     SORCEROR    -> RoleSorceror,
	 SPY         -> RoleSpy,
     
     AUGHUNTER   -> RoleAugHunter,
     SCHOLAR     -> RoleScholar,
     ARCHMAGE    -> RoleArchmage,
  
     FOX         -> RoleFox,
     BETRAYER    -> RoleBetrayer,
     GODFAT      -> RoleGodfat,  

     DEMON       -> RoleDemon,
     FALLEN_ANGEL -> RoleFallenAngel,
	 HERETIC -> RoleHeretic,

     PENGUIN     -> RolePenguin,

     PONTIFF     -> RolePontiff,

     INHERITER   -> RoleInheriter,
     SHIFTER     -> RoleShifter,
     
     CARDMASTER  -> RoleCardMaster
  )

  def HIDDEN_ROLE_LIST = List(
      HERMIT, ALCHEMIST, AUGHUNTER, ARCHMAGE,
      FALLEN_ANGEL, PENGUIN, CARDMASTER, FAIRY, HERETIC, VAMPIRE
  )
  
  def get_role(role : RoleEnum.Value) : RoleData = {
    val result = ROLE_MAP.get(role) 
    if (result.isEmpty)
      println(role.toString + "is null")
    return result.getOrElse(RoleNone)
  }
  
  def get_role(role_string : String) : RoleData = {
    return get_role(valueOf(role_string).getOrElse(NONE))
  }
}

object RoleSpecialEnum extends Enumeration {
  type RoleSpecialEnum = Value

  val NONE        = Value("")

  val POISON      = Value("p")
  val RESIST      = Value("r")
  val CONJURE     = Value("c")
  val WHITE       = Value("w")
  val TEN         = Value("t")
  
  val PRIDE  = Value("o")
  val ENVY  = Value("j")
  val WRATH      = Value("n")
  val LUST          = Value("s")
  val SLOTH         = Value("z")
  val GREED      = Value("g")
  val GLUTTONY  = Value("u")

  def ROLESPECIAL_MAP   = scala.collection.immutable.TreeMap(
     NONE        -> "",

     POISON      -> "毒狼",
     RESIST      -> "抗毒狼",
     CONJURE     -> "咒狼",
     WHITE       -> "白狼",
     TEN         -> "天狼",
	 
	 PRIDE  -> "路西法",
	 ENVY    -> "利維坦",
	 WRATH         -> "撒旦",
	 LUST            -> "莉莉絲",
	 SLOTH          -> "貝利爾",
	 GREED       -> "瑪門",
	 GLUTTONY   -> "別西卜"
  )

  def ROLESPECIAL_WOLF   = scala.collection.immutable.TreeMap(
     POISON      -> "毒",
     RESIST      -> "抗",
     CONJURE     -> "咒",
     WHITE       -> "白",
     TEN         -> "天"
  )
  
  def ROLESPECIAL_FALLENANGEL   = scala.collection.immutable.TreeMap(
	 PRIDE  -> "路西法",
	 ENVY    -> "利維坦",
	 WRATH         -> "撒旦",
	 LUST            -> "莉莉絲",
	 SLOTH          -> "貝利爾",
	 GREED       -> "瑪門",
	 GLUTTONY   -> "別西卜"
  )
  
  def ROLESPECIAL_FALLENANGEL_NOGREED   = scala.collection.immutable.TreeMap(
	 PRIDE  -> "路西法",
	 ENVY    -> "利維坦",
	 WRATH         -> "撒旦",
	 LUST            -> "莉莉絲",
	 SLOTH          -> "貝利爾",
	 GLUTTONY   -> "別西卜"
  )

  def get_string(rolespecial_string : String) : String = {
    val result = ROLESPECIAL_MAP.get(valueOf(rolespecial_string).getOrElse(NONE))
    if (result.isEmpty)
      println(rolespecial_string.toString + "is null")
    return result.getOrElse("")
  }
  
  def get_string_wolf(rolespecial_string : String) : String = {
    val result = ROLESPECIAL_WOLF.get(valueOf(rolespecial_string).getOrElse(NONE))
    if (result.isEmpty)
      println(rolespecial_string.toString + "is null")
    return result.getOrElse("")
  }
}