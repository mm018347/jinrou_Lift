package org.plummtw.jinrou.util

import scala.xml._
import net.liftweb._
import net.liftweb.mapper._
import http._
import js._
import util._
import S._
import SHtml._
//import Helpers._


import org.plummtw.jinrou.model._
import org.plummtw.jinrou.enum._
import org.plummtw.jinrou.data._
import org.plummtw.jinrou.snippet._


object GameProcesser {
  // 死亡及死亡訊息
  def process_death(room:Room, room_day:RoomDay, user: UserEntry, user_entrys: List[UserEntry], mtype: MTypeEnum.Value)  {

    if (user.live.is) {
      if ((user.current_role == RoleEnum.FALLEN_ANGEL) &&
          (user.hasnt_flag(UserEntryFlagEnum.BITED)) &&
          ((mtype == MTypeEnum.DEATH_EATEN) ||
           (mtype == MTypeEnum.DEATH_HUNTER_KILL) ||
           (mtype == MTypeEnum.DEATH_POISON_H) ||
           (mtype == MTypeEnum.DEATH_DEATH_NOTE))) {
        val fallen_users = user_entrys.filter{x => (x.live.is) && (x.has_flag(UserEntryFlagEnum.FALLEN))}

        if (fallen_users.length > 0) {
          val random = new Random
          val random_fallen_user = fallen_users(random.nextInt(fallen_users.length))
          user.user_flags(user.user_flags.is + UserEntryFlagEnum.BITED.toString)
          process_death(room, room_day, random_fallen_user, user_entrys, MTypeEnum.DEATH_FALLENANGEL)
          return
        }
      }

	  user.live(false)
      //x user.save
      val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                    .actioner_id(user.id.is).mtype(mtype.toString)
      sys_mes.save

      if (room.has_flag(RoomFlagEnum.GEMINI_LEGACY) && (user.current_role == RoleEnum.GEMINI)) {
        val live_geminis = user_entrys.filter(x => (x.current_role == RoleEnum.GEMINI) && x.live.is)
        if (live_geminis.length > 0) {
          val legacy_cash = user.cash.is / live_geminis.length
          user.cash(0)
          live_geminis.foreach { live_gemini =>
            live_gemini.cash(live_gemini.cash.is + legacy_cash)

            if ((live_gemini.item_flags.is == ItemEnum.ITEM_NO_ITEM.toString) && (user.item_flags.is != ItemEnum.ITEM_NO_ITEM.toString)) {
              live_gemini.item_flags(user.item_flags.is)
              user.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
            }
          }
        }
      }
    } else {
      Log.warn("RoomDay : " + room_day.id.is.toString + " UserEntry " + user.id.is.toString +
                " Already Dead. New Death : " + mtype.toString)
    }
  }

  // 跟隨死亡
  def process_followers(room:Room, room_day:RoomDay, user_entrys: List[UserEntry]) : Unit = {
    // 若教主全掛，副教主連帶死亡
    val live_pontiff    = user_entrys.filter(x=>(x.current_role == RoleEnum.PONTIFF) && (x.live.is))
    val live_subpontiff = user_entrys.filter(x=>(x.subrole.is == SubroleEnum.SUBPONTIFF.toString) &&
                                                 (x.live.is))
    if (live_pontiff.length == 0) {
      live_subpontiff.foreach { subpontiff =>
        process_death(room, room_day, subpontiff, user_entrys, MTypeEnum.DEATH_SUBPONTIFF)
      }
    }

    // 若狐全掛，背德連帶死亡
    val live_fox      = user_entrys.filter(x=>(x.current_role == RoleEnum.FOX) && (x.live.is))
    val live_betrayer = user_entrys.filter(x=>((x.current_role == RoleEnum.BETRAYER) ||
                                               (x.current_role == RoleEnum.GODFAT) ||
                                               (x.subrole.is == SubroleEnum.FOXBELIEVER.toString)) &&
                                               (x.live.is))
    if (live_fox.length == 0) {
      live_betrayer.foreach { betrayer =>
        process_death(room, room_day, betrayer, user_entrys, MTypeEnum.DEATH_BETRAYER)
      }
    }

    // 人狼全掛，幼狼連帶死亡
    val live_werewolf = user_entrys.filter(x=>(x.current_role == RoleEnum.WEREWOLF) && (x.live.is))
    val live_wolfcub  = user_entrys.filter(x=>(x.current_role == RoleEnum.WOLFCUB)  && (x.live.is))
    if (live_werewolf.length == 0) {
      live_wolfcub.foreach { wolfcub =>
        process_death(room, room_day, wolfcub, user_entrys, MTypeEnum.DEATH_WOLFCUB)
      }
    }

    // 惡魔靈魂檢測
    val live_demons = user_entrys.filter(x=>(x.current_role == RoleEnum.DEMON) && (x.live.is) &&
                                           (x.action_point.is + user_entrys.filter(x=> !x.live.is).length > 35))
    if (live_demons.length != 0) {
      live_demons.foreach { live_demon =>
        process_death(room, room_day, live_demon, user_entrys, MTypeEnum.DEATH_SUDDEN)
      }
    }
	
	// 異端鮮血檢測
    val live_heretics = user_entrys.filter(x=>(x.current_role == RoleEnum.HERETIC) && (x.live.is) &&
                                           (x.action_point.is == 0))
    if (live_heretics.length != 0) {
      live_heretics.foreach { live_heretic =>
        process_death(room, room_day, live_heretic, user_entrys, MTypeEnum.DEATH_SUDDEN)
      }
    }

    if ((room.has_flag(RoomFlagEnum.SHIFTER_LINKS)) &&
        (room_day.day_no.is < 12)) {
      // 模仿師生命連結檢測
      val live_links = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LINKS) ) && (x.live.is))
      if (live_links.length == 1) {
        process_death(room, room_day, live_links(0), user_entrys, MTypeEnum.DEATH_LINKS)
        process_followers(room, room_day, user_entrys)
      }

      /*
       // 若教主全掛，副教主連帶死亡
      val live_pontiff2    = user_entrys.filter(x=>(x.current_role == RoleEnum.PONTIFF) && (x.live.is))
      val live_subpontiff2 = user_entrys.filter(x=>(x.subrole.is == SubroleEnum.SUBPONTIFF.toString) &&
                                                 (x.live.is))
      if (live_pontiff2.length == 0) {
        live_subpontiff2.foreach { subpontiff =>
          process_death(room, room_day, subpontiff, user_entrys, MTypeEnum.DEATH_SUBPONTIFF)
        }
      }

      // 若狐全掛，背德連帶死亡
      val live_fox2      = user_entrys.filter(x=>(x.current_role == RoleEnum.FOX) && (x.live.is))
      val live_betrayer2 = user_entrys.filter(x=>((x.current_role == RoleEnum.BETRAYER) ||
                                                 (x.current_role == RoleEnum.GODFAT)||
                                                 (x.subrole.is == SubroleEnum.FOXBELIEVER.toString)) &&
                                                 (x.live.is))
      if (live_fox2.length == 0) {
        live_betrayer2.foreach { betrayer =>
          process_death(room, room_day, betrayer, user_entrys, MTypeEnum.DEATH_BETRAYER)
        }
      }

      // 人狼全掛，幼狼連帶死亡
      val live_werewolf2 = user_entrys.filter(x=>(x.current_role == RoleEnum.WEREWOLF) && (x.live.is))
      val live_wolfcub2  = user_entrys.filter(x=>(x.current_role == RoleEnum.WOLFCUB)  && (x.live.is))
      if (live_werewolf2.length == 0) {
        live_wolfcub2.foreach { wolfcub =>
          process_death(room, room_day, wolfcub, user_entrys, MTypeEnum.DEATH_WOLFCUB)
        }
      }

      // 戀人檢測
      val live_links2 = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LINKS) ) && (x.live.is))
      if (live_links2.length == 1)
        process_death(room, room_day, live_links2(0), user_entrys, MTypeEnum.DEATH_LINKS)

      // 惡魔靈魂檢測
      val live_demons2 = user_entrys.filter(x=>(x.current_role == RoleEnum.DEMON) && (x.live.is) &&
                                               (x.action_point.is + user_entrys.filter(x=> !x.live.is).length > 35))
      if (live_demons2.length != 0) {
        live_demons2.foreach { live_demon =>
          process_death(room, room_day, live_demon, user_entrys, MTypeEnum.DEATH_SUDDEN)
        }
      }
      */
    }

    val live_lovers = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LOVER) ) && (x.live.is))
    if (live_lovers.length == 1) {
      process_death(room, room_day, live_lovers(0), user_entrys, MTypeEnum.DEATH_LOVER)
      process_followers(room, room_day, user_entrys)
    }
  }

  // 分配職業
  def dispatch_role(room: Room, user_entrys:List[UserEntry]) {
    val random = new Random()
    // 這邊我為了方便處理起見，不使用 Scala 的 List，改用 Java 的 List
    val user_entrys_size = user_entrys.length
    
    // 先產生職業清單
    var role_array : java.util.LinkedList[String] = new java.util.LinkedList()
    if (user_entrys_size >=  8) role_array.add(RoleEnum.AUGURER.toString)
    if (user_entrys_size >=  9) role_array.add(RoleEnum.NECROMANCER.toString)
    if (user_entrys_size >= 10) role_array.add(RoleEnum.MADMAN.toString)
    if (user_entrys_size >= 11) role_array.add(RoleEnum.HUNTER.toString)
    if (user_entrys_size >= 13) {
      role_array.add(RoleEnum.GEMINI.toString)
      role_array.add(RoleEnum.GEMINI.toString)
    }
    if ((user_entrys_size >= 14) && (room.has_flag(RoomFlagEnum.ROLE_INHERITER))) {
      if (room.has_flag(RoomFlagEnum.GM_HERMIT1))
        role_array.add(RoleEnum.HERMIT.toString)
      else
        role_array.add(RoleEnum.INHERITER.toString)
    }
    if (user_entrys_size >= 15) role_array.add(RoleEnum.FOX.toString)
    if ((user_entrys_size >= 17) && (room.has_flag(RoomFlagEnum.ROLE_CLERIC)))
      role_array.add(RoleEnum.CLERIC.toString)
    if ((user_entrys_size >= 18) && (room.has_flag(RoomFlagEnum.ROLE_HERBALIST)))
      role_array.add(RoleEnum.HERBALIST.toString)
    if ((user_entrys_size >= 20) && (room.has_flag(RoomFlagEnum.ROLE_POISONER)))
      role_array.add(RoleEnum.POISONER.toString)
    if ((user_entrys_size >= 20) && (room.has_flag(RoomFlagEnum.ROLE_BETRAYER)) &&
                                    (room.room_flags.is.indexOf(RoomFlagEnum.FOX_OPTION1.toString) == -1) )
      role_array.add(RoleEnum.BETRAYER.toString)
    if ((user_entrys_size >= 21) && (room.has_flag(RoomFlagEnum.ROLE_RUNNER)))
      role_array.add(RoleEnum.RUNNER.toString)
    if ((user_entrys_size >= 22) && (room.has_flag(RoomFlagEnum.ROLE_SORCEROR)))
      role_array.add(RoleEnum.SORCEROR.toString)
    if ((user_entrys_size >= 23) && (room.has_flag(RoomFlagEnum.ROLE_WOLFCUB)))
      role_array.add(RoleEnum.WOLFCUB.toString)
    if ((user_entrys_size >= 23) && (room.has_flag(RoomFlagEnum.ROLE_DEMON)))
    {
      if (room.has_flag(RoomFlagEnum.GM_PENGUIN1))
        role_array.add(RoleEnum.PENGUIN.toString)
      else
        role_array.add(RoleEnum.DEMON.toString)
    }
      
    if ((user_entrys_size >= 24) && (room.has_flag(RoomFlagEnum.ROLE_SCHOLAR)))
      role_array.add(RoleEnum.SCHOLAR.toString)
    if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.ROLE_GODFAT)))
      role_array.add(RoleEnum.GODFAT.toString)
    if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.ROLE_SHIFTER)))
      role_array.add(RoleEnum.SHIFTER.toString)
    if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.ROLE_PONTIFF))) {
      if (room.has_flag(RoomFlagEnum.GM_PONTIFF1))
        role_array.add(RoleEnum.FALLEN_ANGEL.toString)
      else
        role_array.add(RoleEnum.PONTIFF.toString)
    }

    val werewolf_number = ((user_entrys_size + 2)/5) -
      (if ((user_entrys_size >= 23) && (room.has_flag(RoomFlagEnum.ROLE_WOLFCUB))) 1 else 0)
    for (i <- 1 to werewolf_number)   // 狼數公式
      role_array.add(RoleEnum.WEREWOLF.toString)
    
    for (i <- 1 to (user_entrys_size - role_array.size())) // 剩下的補村民
      role_array.add(RoleEnum.VILLAGER.toString)

      
    // 設定玩家優先順位
    var user_no_array : java.util.LinkedList[Int] = new java.util.LinkedList()
    for (i <- 1 to user_entrys_size)
      user_no_array.add(i)
      
    java.util.Collections.shuffle(user_no_array)
    
    user_entrys.foreach { user_entry =>
      user_entry.user_no(user_no_array.removeFirst()).user_flags(user_entry.role.is).role("")
      //println(user_entry.user_no.is + " " + user_entry.user_flags.is)
    }

    val user_entrys_ordered = user_entrys.sort(_.user_no.is < _.user_no.is)
    // 邱比特初始
	// (x.subrole.is != SubroleEnum.HASHIHIME.toString)
	/*
    if (room.has_flag(RoomFlagEnum.CUBIC_INIT)) {
      user_entrys_ordered(0).item_flags(ItemEnum.CUBIC_ARROW.toString).cash(0)
    }
	*/
	if ((user_entrys_size >= 25) &&
        (room.has_flag(RoomFlagEnum.CUBIC_INIT)) &&
		(!room.has_flag(RoomFlagEnum.LOVER_LETTER_EXCHANGE))) {
      var cubic_array_lover : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_no_lover = user_entrys.filter(x=>(x.uname.is != "dummy_boy") && (x.subrole.is != SubroleEnum.HASHIHIME.toString)).map(_.user_no.is)
      user_no_lover.foreach{i =>
        cubic_array_lover.add(i)
      }
      java.util.Collections.shuffle(cubic_array_lover)
      var cubic_lover = cubic_array_lover.removeFirst()
      var cubic_lover_users = user_entrys.filter(_.user_no.is == cubic_lover)
      if (cubic_lover_users.length >= 1){
          cubic_lover_users(0).item_flags(ItemEnum.CUBIC_ARROW.toString).cash(0)
		}
    }

    // 第一次先看看有沒有希望職業
    user_entrys_ordered.foreach(user_entry =>
      if (role_array.contains(user_entry.user_flags.is) && (random.nextInt(6) != 0)) {
        //println(user_entry.user_no.is + ":" + user_entry.user_flags.is)
        user_entry.role(user_entry.user_flags.is).user_flags("")
        role_array.remove(user_entry.role.is)
      }
    )

    java.util.Collections.shuffle(role_array)
    
    // 然後設定剩下的職業
    user_entrys_ordered.foreach(user_entry =>
      if (user_entry.role.is == "") {
        user_entry.role(role_array.removeFirst()).user_flags("")
      }
    )

    // 人狼特性
    if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.WEREWOLF_OPTION1))) {
      val role_special_list = RoleSpecialEnum.ROLESPECIAL_WOLF.keys.toList
      user_entrys.foreach{ user_entry =>
        if (user_entry.current_role == RoleEnum.WEREWOLF) {
          if (random.nextInt(5) < 3) {
            val role_special = role_special_list(random.nextInt(role_special_list.length))
            user_entry.role(RoleEnum.WEREWOLF.toString + role_special)
          }
        }
      }
    }

    // 先設定人狼專用副職業
    var subrole_array_werewolf : java.util.LinkedList[Int] = new java.util.LinkedList()
    val user_werewolf = user_entrys.filter(x=>(x.current_role == RoleEnum.WEREWOLF))
    user_werewolf.foreach{i =>
      subrole_array_werewolf.add(i.user_no.is)
    }
    java.util.Collections.shuffle(subrole_array_werewolf)

    if ((user_entrys_size >= 20) && (room.has_flag(RoomFlagEnum.SUBROLE_ALPHAWOLF))) {
      val sub_alphawolf = subrole_array_werewolf.removeFirst()
      user_entrys.filter(_.user_no.is == sub_alphawolf)(0).subrole(SubroleEnum.ALPHAWOLF.toString)
    }
    if ((user_entrys_size >= 20) && (room.has_flag(RoomFlagEnum.SUBROLE_WISEWOLF))) {
      val sub_wisewolf = subrole_array_werewolf.removeFirst()
      user_entrys.filter(_.user_no.is == sub_wisewolf)(0).subrole(SubroleEnum.WISEWOLF.toString)
    }

    // 設定人側專用副職業
    if (room.has_flag(RoomFlagEnum.SUBROLE_FAKEAUGURER)) {
      var subrole_array_villager : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_villager_side = user_entrys.filter(x=>((RoleEnum.get_role(x.current_role).role_side == RoomVictoryEnum.VILLAGER_WIN) && (x.role.is != RoleEnum.AUGURER.toString)))
      user_villager_side.foreach{i =>
        subrole_array_villager.add(i.user_no.is)
      }
      java.util.Collections.shuffle(subrole_array_villager)

      val sub_fakeaugurer = subrole_array_villager.removeFirst()
      user_entrys.filter(_.user_no.is == sub_fakeaugurer)(0).subrole(SubroleEnum.FAKEAUGURER.toString)
    }

    if (room.has_flag(RoomFlagEnum.SUBROLE_WOLFSTAMP)) {
      var subrole_array_villager : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_villager_side = user_entrys.filter(x=>(RoleEnum.get_role(x.current_role).role_side != RoomVictoryEnum.WEREWOLF_WIN) &&
                                                     (x.subrole.is == ""))
      user_villager_side.foreach{i =>
        subrole_array_villager.add(i.user_no.is)
      }
      java.util.Collections.shuffle(subrole_array_villager)

      val wolfstamp = subrole_array_villager.removeFirst()
      user_entrys.filter(_.user_no.is == wolfstamp)(0).subrole(SubroleEnum.WOLFSTAMP.toString)
    }


    //val sub_wolfstamp = subrole_array_villager.removeFirst()
    //user_entrys.filter(_.user_no.is == sub_wolfstamp)(0).subrole(SubroleEnum.WOLFSTAMP.toString)
    
    // 設定副職業
    var subrole_array : java.util.LinkedList[Int] = new java.util.LinkedList()
    val user_normal_subrole = user_entrys.filter(x=>(x.subrole.is == ""))
    user_normal_subrole.foreach{i =>
      subrole_array.add(i.user_no.is)
    }
    //for (i <- 1 to user_entrys_size)
    //  subrole_array.add(i)
    java.util.Collections.shuffle(subrole_array)

    var subrole_array_plus : java.util.LinkedList[String] = new java.util.LinkedList()
    if ((user_entrys_size >= 16) && (room.has_flag(RoomFlagEnum.SUBROLE_PLUS))) {
      subrole_array_plus.add(SubroleEnum.AUTHORITY.toString)
      subrole_array_plus.add(SubroleEnum.DECIDER.toString)

      if ((user_entrys_size >= 16) && (room.has_flag(RoomFlagEnum.SUBROLE_AVENGER)))
         subrole_array_plus.add(SubroleEnum.AVENGER.toString)

      if ((user_entrys_size >= 20) && 
          ((room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS4)) ||
           (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS4_2))))
         subrole_array_plus.add(SubroleEnum.MEMORYLOSS4.toString)

      if ((user_entrys_size >= 24) && (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS6)))
         subrole_array_plus.add(SubroleEnum.MEMORYLOSS6.toString)

      if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS8)))
         subrole_array_plus.add(SubroleEnum.MEMORYLOSS8.toString)

      if ((user_entrys_size >= 22) && (room.has_flag(RoomFlagEnum.SUBROLE_SUDDENDEATH)))
         subrole_array_plus.add(SubroleEnum.SUDDENDEATH.toString)

      //if ((user_entrys_size >= 18) && (room.has_flag(RoomFlagEnum.SUBROLE_WOLFBELIEVER)))
      //   subrole_array_plus.add(SubroleEnum.WOLFBELIEVER.toString)
    }

    val subrole_plus =
      if (subrole_array_plus.size() > 0)
        subrole_array_plus.get(random.nextInt(subrole_array_plus.size()))
      else
        null

    if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.SUBROLE_HASHIHIME))) {
      val hashihime = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == hashihime)(0).subrole(SubroleEnum.HASHIHIME.toString)
	  if ((room.has_flag(RoomFlagEnum.CUBIC_INIT)) && (room.has_flag(RoomFlagEnum.LOVER_INIT)) && (!room.has_flag(RoomFlagEnum.LOVER_LETTER_EXCHANGE)) ) {
	    val hashihime2 = subrole_array.removeFirst()
	    user_entrys.filter(_.user_no.is == hashihime2)(0).subrole(SubroleEnum.HASHIHIME.toString)
	   }
    }

    if (user_entrys_size >= 16) {
      val subrole_authority = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == subrole_authority)(0).subrole(SubroleEnum.AUTHORITY.toString)
      
      val subrole_decider = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == subrole_decider)(0).subrole(SubroleEnum.DECIDER.toString)

      if (room.has_flag(RoomFlagEnum.SUBROLE_AVENGER)) {
        val avenger = subrole_array.removeFirst()
        user_entrys.filter(_.user_no.is == avenger)(0).subrole(SubroleEnum.AVENGER.toString)
      }
    }

    if ((user_entrys_size >= 20) && (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS4))) {
      val memory_loss4 = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == memory_loss4)(0).subrole(SubroleEnum.MEMORYLOSS4.toString)
    }

    if ((user_entrys_size >= 20) && (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS4_2))) {
      val memory_loss4 = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == memory_loss4)(0).subrole(SubroleEnum.MEMORYLOSS4.toString)
    }

    if ((user_entrys_size >= 24) && (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS6))) {
      val memory_loss6 = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == memory_loss6)(0).subrole(SubroleEnum.MEMORYLOSS6.toString)
    }

    if ((user_entrys_size >= 25) && (room.has_flag(RoomFlagEnum.SUBROLE_MEMORYLOSS8))) {
      val memory_loss8 = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == memory_loss8)(0).subrole(SubroleEnum.MEMORYLOSS8.toString)
    }

    if ((subrole_plus != null) && (subrole_plus != SubroleEnum.SUDDENDEATH.toString) &&
        (subrole_plus != SubroleEnum.WOLFBELIEVER.toString)) {
      val subrole_plus_no = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == subrole_plus_no)(0).subrole(subrole_plus)
    }

    // 移除 Fox Demon Pontiff
    val user_no_for_remove = user_entrys.filter(x=>(x.role.is == RoleEnum.FOX.toString) ||
                                 (x.role.is == RoleEnum.DEMON.toString) ||
                                 (x.role.is == RoleEnum.PENGUIN.toString) ||
                                 (x.role.is == RoleEnum.PONTIFF.toString)).map(_.user_no.is)
    var subrole_array2 : java.util.LinkedList[Int] = new java.util.LinkedList()
    user_no_for_remove.foreach(subrole_array2.add(_))
    subrole_array.removeAll(subrole_array2)

    if ((user_entrys_size >= 22) && (room.has_flag(RoomFlagEnum.SUBROLE_SUDDENDEATH))) {
      val sudden_death = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == sudden_death)(0).subrole(SubroleEnum.SUDDENDEATH.toString)
    }
/*
    if ((user_entrys_size >= 18) && (room.has_flag(RoomFlagEnum.SUBROLE_WOLFBELIEVER))) {
      val wolf_believer = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == wolf_believer)(0).subrole(SubroleEnum.WOLFBELIEVER.toString)
    }
*/
    if ((subrole_plus != null) && ((subrole_plus == SubroleEnum.SUDDENDEATH.toString) ||
        (subrole_plus == SubroleEnum.WOLFBELIEVER.toString))) {
      val subrole_plus_no = subrole_array.removeFirst()
      user_entrys.filter(_.user_no.is == subrole_plus_no)(0).subrole(subrole_plus)
    }
	
	// 異端選項2+1
	if (user_entrys_size >= 25) {
      if (room.has_flag(RoomFlagEnum.GM_HERETIC2)) {
        val villagers = user_entrys.filter(_.role.is == RoleEnum.INHERITER.toString)
        if (villagers.length >= 1) {
          villagers(0).role(RoleEnum.HERETIC.toString)
	  	  villagers(0).action_point(1)
        }
      }else if (room.has_flag(RoomFlagEnum.GM_HERETIC1)) {
        val villagers = user_entrys.filter(_.role.is == RoleEnum.VILLAGER.toString)
        if (villagers.length >= 1) {
	        villagers(0).role(RoleEnum.HERETIC.toString)
		    villagers(0).action_point(1)
        }
    }
	}
	
	// 間諜選項1
	if (user_entrys_size >= 16) {
      if (room.has_flag(RoomFlagEnum.GM_SPY1)) {
        val villagers = user_entrys.filter(_.role.is == RoleEnum.MADMAN.toString)
        if (villagers.length >= 1) {
          villagers(0).role(RoleEnum.SPY.toString)
	  	  villagers(0).action_point(3)
        }
      }
	}
	
	if ((user_entrys_size >= 18) &&
        (room.has_flag(RoomFlagEnum.SUBROLE_WOLFBELIEVER))) {
      // 設定狼信者
      var subrole_array_wolfbeliever : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_no_wolf = user_entrys.filter(x=>(x.current_role != RoleEnum.WEREWOLF) &&
                                                 (x.role.is != RoleEnum.WOLFCUB.toString) &&
												 (x.role.is != RoleEnum.SORCEROR.toString) &&
												 (x.role.is != RoleEnum.MADMAN.toString) &&
												 (x.role.is != RoleEnum.PONTIFF.toString) &&
                                                 (x.role.is != RoleEnum.FOX.toString) &&
                                                 (x.role.is != RoleEnum.DEMON.toString) &&
												 (x.role.is != RoleEnum.PENGUIN.toString) &&
												 (x.role.is != RoleEnum.HERETIC.toString) &&
												 (x.role.is != RoleEnum.SPY.toString) &&
                                                 (x.subrole.is == "")).map(_.user_no.is)
      user_no_wolf.foreach{i =>
        subrole_array_wolfbeliever.add(i)
      }
      java.util.Collections.shuffle(subrole_array_wolfbeliever)
      val sub_wolfbeliever = subrole_array_wolfbeliever.removeFirst()
      user_entrys.filter(_.user_no.is == sub_wolfbeliever)(0).subrole(SubroleEnum.WOLFBELIEVER.toString)
    }

    if ((user_entrys_size >= 25) &&
        (room.has_flag(RoomFlagEnum.ROLE_PONTIFF)) &&
        (!room.has_flag(RoomFlagEnum.GM_PONTIFF1)) &&
        (room.has_flag(RoomFlagEnum.SUBROLE_SUBPONTIFF))) {
      // 設定副教主
      var subrole_array_subpontiff : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_no_scholar = user_entrys.filter(x=>(x.role.is != RoleEnum.PONTIFF.toString) &&
                                                 (x.role.is != RoleEnum.SCHOLAR.toString) &&
                                                 (x.role.is != RoleEnum.FOX.toString) &&
                                                 (x.role.is != RoleEnum.DEMON.toString) &&
												 (x.role.is != RoleEnum.HERETIC.toString) &&
                                                 (x.subrole.is == "")).map(_.user_no.is)
      user_no_scholar.foreach{i =>
        subrole_array_subpontiff.add(i)
      }
      java.util.Collections.shuffle(subrole_array_subpontiff)
      val sub_pontiff = subrole_array_subpontiff.removeFirst()
      user_entrys.filter(_.user_no.is == sub_pontiff)(0).subrole(SubroleEnum.SUBPONTIFF.toString).user_flags(user_entrys.filter(_.user_no.is == sub_pontiff)(0).user_flags.is.toString + UserEntryFlagEnum.RELIGION.toString)

      // 設定無神論者
	  if (!(room.has_flag(RoomFlagEnum.GM_HERETIC1)) || !(room.has_flag(RoomFlagEnum.GM_HERETIC2))){
      var subrole_array_noreligion : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_no_cleric = user_entrys.filter(x=>(x.role.is != RoleEnum.PONTIFF.toString) &&
                                                 (x.role.is != RoleEnum.CLERIC.toString) &&
                                                 (x.subrole.is == "")).map(_.user_no.is)
      user_no_cleric.foreach{i =>
        subrole_array_noreligion.add(i)
      }
      java.util.Collections.shuffle(subrole_array_noreligion)
      var sub_noreligion = subrole_array_noreligion.removeFirst()
      var sub_noreligion_users = user_entrys.filter(_.user_no.is == sub_noreligion)
      if (sub_noreligion_users.length >= 1)
        sub_noreligion_users(0).subrole(SubroleEnum.NORELIGION.toString).user_flags(sub_noreligion_users(0).user_flags.is.toString + UserEntryFlagEnum.NORELIGION.toString)
      sub_noreligion = subrole_array_noreligion.removeFirst()
      sub_noreligion_users = user_entrys.filter(_.user_no.is == sub_noreligion)
      if (sub_noreligion_users.length >= 1)
        sub_noreligion_users(0).subrole(SubroleEnum.NORELIGION.toString).user_flags(sub_noreligion_users(0).user_flags.is.toString + UserEntryFlagEnum.NORELIGION.toString)
      }
	}

    val user_dummy = user_entrys.filter(x => x.uname.is == "dummy_boy")
    
    // 如果替身君是 狼 狐 毒 惡魔 則要換掉
    if ((user_dummy.length == 1) &&
        ((user_dummy(0).current_role == RoleEnum.WEREWOLF)||
         (user_dummy(0).current_role == RoleEnum.WOLFCUB)||
         (user_dummy(0).current_role == RoleEnum.FOX)||
         (user_dummy(0).current_role == RoleEnum.POISONER)||
         (user_dummy(0).current_role == RoleEnum.DEMON) ||
         (user_dummy(0).current_role == RoleEnum.FALLEN_ANGEL) ||
         (user_dummy(0).current_role == RoleEnum.PENGUIN) ||
         ((user_dummy(0).current_role == RoleEnum.PONTIFF) &&
         (room.has_flag(RoomFlagEnum.SUBROLE_SUBPONTIFF))))) {
      val temp_user = user_entrys_ordered.filter(x => 
        (x.current_role != RoleEnum.WEREWOLF) &&
        (x.current_role != RoleEnum.WOLFCUB) &&
        (x.current_role != RoleEnum.FOX) &&
        (x.current_role != RoleEnum.POISONER) &&
        (x.current_role != RoleEnum.DEMON) &&
        (x.current_role != RoleEnum.DEMON) &&
        (x.current_role != RoleEnum.PENGUIN) &&
        ((x.current_role != RoleEnum.PONTIFF) ||
         (room.room_flags.is.indexOf(RoomFlagEnum.SUBROLE_SUBPONTIFF.toString) == -1))
      )
      val temp_role = temp_user(0).role.is
      val temp_subrole = temp_user(0).subrole.is
      val temp_user_flags = temp_user(0).user_flags.is
      temp_user(0).role(user_dummy(0).role.is)
      temp_user(0).subrole(user_dummy(0).subrole.is)
      temp_user(0).user_flags(user_dummy(0).user_flags.is)
      user_dummy(0).role(temp_role)
      user_dummy(0).subrole(temp_subrole)
      user_dummy(0).user_flags(temp_user_flags)
    }

    // 教主選項 2
    if ((room.has_flag(RoomFlagEnum.ROLE_PONTIFF)) &&
        (room.has_flag(RoomFlagEnum.PONTIFF_OPTION2))) {
      val pontiff    = user_entrys.filter(x=>(x.current_role == RoleEnum.PONTIFF))
      if (pontiff.length != 0) {
        user_entrys.filter(x=>(x.role.is == RoleEnum.CLERIC.toString)).foreach(_.user_flags(UserEntryFlagEnum.RELIGION.toString))
        user_entrys.filter(x=>(x.role.is == RoleEnum.SCHOLAR.toString)).foreach(_.user_flags(UserEntryFlagEnum.NORELIGION.toString))
      }
    }

    // 教主選項 3
    if ((room.has_flag(RoomFlagEnum.ROLE_PONTIFF)) &&
        (room.has_flag(RoomFlagEnum.PONTIFF_OPTION3) )) {
      user_entrys.filter(x=>(x.role.is == RoleEnum.PONTIFF.toString)).foreach(_.user_flags(UserEntryFlagEnum.PONTIFF_AURA.toString))
    }
	
	// 設定戀人
	if ((user_entrys_size >= 25) &&
        (room.has_flag(RoomFlagEnum.LOVER_INIT))) {
      var flag_array_lover : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_no_lover = user_entrys.filter(x=>(x.uname.is != "dummy_boy") && (x.user_flags.is != UserEntryFlagEnum.LOVER.toString) && (x.subrole.is != SubroleEnum.HASHIHIME.toString)).map(_.user_no.is)
      user_no_lover.foreach{i =>
        flag_array_lover.add(i)
      }
      java.util.Collections.shuffle(flag_array_lover)
      var flag_lover = flag_array_lover.removeFirst()
      var flag_lover_users = user_entrys.filter(_.user_no.is == flag_lover)
      if (flag_lover_users.length >= 1){
          flag_lover_users(0).user_flags(flag_lover_users(0).user_flags.is.toString + UserEntryFlagEnum.LOVER.toString)
		}
      flag_lover = flag_array_lover.removeFirst()
      flag_lover_users = user_entrys.filter(_.user_no.is == flag_lover)
      if (flag_lover_users.length >= 1){
          flag_lover_users(0).user_flags(flag_lover_users(0).user_flags.is.toString + UserEntryFlagEnum.LOVER.toString)
		}
	  if((random.nextInt(20) == 0) && (!room.has_flag(RoomFlagEnum.LOVER_LETTER_EXCHANGE))){
	  flag_lover = flag_array_lover.removeFirst()
      flag_lover_users = user_entrys.filter(_.user_no.is == flag_lover)
      if (flag_lover_users.length >= 1){
          flag_lover_users(0).user_flags(flag_lover_users(0).user_flags.is.toString + UserEntryFlagEnum.LOVER.toString)
		}
	  }
    }

    // 背德初期票數
    user_entrys.filter(x=>(x.role.is == RoleEnum.BETRAYER.toString)).foreach(_.action_point(1))

    // 特殊村選項1
    if (room.has_flag(RoomFlagEnum.ARCHMAGE_OPTION1)) {
      val villagers = user_entrys.filter(_.role.is == RoleEnum.VILLAGER.toString)
      if (villagers.length >= 1) {
	      villagers(0).role(RoleEnum.ARCHMAGE.toString)
          villagers(0).user_flags(villagers(0).user_flags.is.toString + UserEntryFlagEnum.WATER_ELEM_USED.toString)
		}
    }

    // 特殊村選項2
    if (room.has_flag(RoomFlagEnum.CARDMASTER_OPTION1)) {
      val villagers = user_entrys.filter(_.role.is == RoleEnum.SHIFTER.toString)
      if (villagers.length >= 1) {
        villagers(0).role(RoleEnum.CARDMASTER.toString)
      }
    }
	
	// 墮天使特性
    if (room.has_flag(RoomFlagEnum.FALLENANGEL_OPTION1)) {
      val pontiff    = user_entrys.filter(x=>(x.current_role == RoleEnum.PONTIFF))
	  if((room.has_flag(RoomFlagEnum.ITEM_MODE)) || (pontiff.length != 0)) {
        val role_special_list = RoleSpecialEnum.ROLESPECIAL_FALLENANGEL.keys.toList
        user_entrys.foreach{ user_entry =>
          if (user_entry.current_role == RoleEnum.FALLEN_ANGEL) {
            val role_special = role_special_list(random.nextInt(role_special_list.length))
            user_entry.role(RoleEnum.FALLEN_ANGEL.toString + role_special)
          }
        }
      }else {
	    val role_special_list = RoleSpecialEnum.ROLESPECIAL_FALLENANGEL_NOGREED.keys.toList
        user_entrys.foreach{ user_entry =>
          if (user_entry.current_role == RoleEnum.FALLEN_ANGEL) {
            val role_special = role_special_list(random.nextInt(role_special_list.length))
            user_entry.role(RoleEnum.FALLEN_ANGEL.toString + role_special)
          }
        }
	  }
    }
  }

  def generate_item(room : Room, user_entrys: List[UserEntry]) = {
    // 計算競標道具
    val random   = new Random()
    var new_item = ItemEnum.ITEM_NO_ITEM
    if (room.has_flag(RoomFlagEnum.ITEM_MODE)) {
      val item_list = ItemEnum.ITEM_MAP.values.toList
      val total_weight = item_list.map(_.weight).reduceLeft[Int](_+_)
      var random_weight = random.nextInt(total_weight)
      //println("Total_Weight : " + total_weight)

      ItemEnum.ITEM_MAP.keys.foreach { item =>
        //println("Random_Weight : " + random_weight)
        val item_weight = ItemEnum.get_item(item).weight
        //println("Item [" + item.toString + "] Weight : " + item_weight)

        if ((random_weight < item_weight) && (random_weight >= 0))
          new_item = item
        random_weight -= item_weight
        //println("New Item : " + new_item.toString)
      }
    }

    if (new_item == ItemEnum.CUBIC_ARROW) {
      if ((!room.has_flag(RoomFlagEnum.ITEM_CUBIC)) || (room.has_flag(RoomFlagEnum.LOVER_LETTER_EXCHANGE)))
        new_item = ItemEnum.PANDORA_BOX
      else {
        val lover_users = user_entrys.filter ( x => (x.has_flag(UserEntryFlagEnum.LOVER)) || 
                                                    (x.item_flags.is == ItemEnum.CUBIC_ARROW.toString) )
        if ((lover_users.length != 0) && (random.nextInt(2) == 0))
          new_item = ItemEnum.PANDORA_BOX
      }
    }

    new_item
  }

  def process_start_game(room:Room, user_entrys:List[UserEntry]) = {
    GameProcesser.dispatch_role(room, user_entrys)
    user_entrys.foreach(_.save)

    // 加入第一夜
    val new_day = RoomDay.create.room_id(room.id.is).day_no(1).vote_time(1).item(generate_item(room, user_entrys).toString)
    new_day.save()

    // 產生人數字串
    val role_list = RoleEnum.ROLE_MAP.keys.toList.filter(_ != RoleNone)
    var role_text = new StringBuffer("")
    role_list.foreach{role =>
      var role_number = user_entrys.filter(_.current_role == role).length

      // 這邊要處理妖狐指定背德時的特殊處理
      if ((user_entrys.length >= 20) &&
          (room.has_flag(RoomFlagEnum.ROLE_BETRAYER)) &&
          (room.has_flag(RoomFlagEnum.FOX_OPTION1))) {
        if (role == RoleEnum.BETRAYER)
          role_number = role_number + 1
        else if (role == RoleEnum.VILLAGER)
          role_number = role_number - 1
      }
	  
	  if (room.has_flag(RoomFlagEnum.FALLENANGEL_OPTION1)) {
      if (role == RoleEnum.FALLEN_ANGEL)
          role_number = role_number - 1
        }

      if (role_number > 0) {
        role_text.append("　")
        role_text.append(RoleEnum.get_role(role).role_name)
        role_text.append(" ")
        role_text.append(role_number.toString)
      }
    }

      if (room.has_flag(RoomFlagEnum.FALLENANGEL_OPTION1)) {
        val user_entrys_ordered = user_entrys.sort(_.user_no.is < _.user_no.is)
        user_entrys_ordered.foreach { user_entry_ordered =>
          if ((user_entry_ordered.current_role == RoleEnum.FALLEN_ANGEL) &&
              (user_entry_ordered.role.is.toString.length > 1)) {
            val rolespecial_enum = RoleSpecialEnum.get_string(user_entry_ordered.role.is.substring(1,2))
            if (rolespecial_enum != RoleSpecialEnum.NONE.toString)
              role_text.append("　")
              role_text.append(rolespecial_enum)
              role_text.append(" 1")
            if (rolespecial_enum == RoleSpecialEnum.NONE.toString)
              role_text.append("　墮天使 1")
          }
        }
      }

    val subrole_list = SubroleEnum.SUBROLE_MAP.keys.toList.filter(_ != SubroleNone)
    subrole_list.foreach{subrole =>
      val subrole_number = user_entrys.filter(_.subrole.is == subrole.toString).length
      if (subrole_number > 0) {
        role_text.append("　(")
        val subrole_data = SubroleEnum.get_subrole(subrole)
        val subrole_name = subrole_data.subrole_name
        /*  if (subrole_data == SubroleAlphaWolf)
            "大狼"
          else if (subrole_data == SubroleFakeAugurer)
            "冒牌占"
          else
            subrole_data.subrole_name */
        role_text.append(subrole_name)
        role_text.append(" ")
        role_text.append(subrole_number.toString)
        role_text.append(")")
      }
    }


    if ((user_entrys.length >= 25) && (room.has_flag(RoomFlagEnum.WEREWOLF_OPTION1))) {
      role_text.append("　(")
      val user_entrys_ordered = user_entrys.sort(_.user_no.is < _.user_no.is)
      user_entrys_ordered.foreach { user_entry_ordered =>
        if ((user_entry_ordered.current_role == RoleEnum.WEREWOLF) &&
            (user_entry_ordered.role.is.toString.length > 1)) {
          val rolespecial_enum = RoleSpecialEnum.get_string_wolf(user_entry_ordered.role.is.substring(1,2))
          if (rolespecial_enum != RoleSpecialEnum.NONE.toString)
            role_text.append(rolespecial_enum)
        }
      }
      role_text.append(")")
    }
	
	if (room.has_flag(RoomFlagEnum.LOVER_INIT)) {
	role_text.append("　{戀人 ")
	var total_lover = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LOVER)))
    role_text.append(total_lover.length.toString)
	role_text.append("}")
	}

    val talk = Talk.create.roomday_id(new_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                           .message(role_text.toString).font_type("12")
    talk.save()

    room.status(RoomStatusEnum.PLAYING.toString)
    room.save()
  }
  
  def process_day(room:Room, room_day:RoomDay, user_entrys:List[UserEntry], votes:List[Vote], voted_player:UserEntry) : RoomVictoryEnum.Value = {
    val random                           = new Random()
    //var users_for_save : List[UserEntry] = List()
    var talks_for_save : List[Talk]      = List()


    user_entrys.foreach { user =>
      if (user.item_flags.is == ItemEnum.PANDORA_BOX.toString) {
        val new_item = (random.nextInt(12) match {
          case  0 => ItemEnum.UNLUCKY_PURSE
          case  1 => ItemEnum.BLESS_STAFF
          case  2 => ItemEnum.BLACK_FEATHER
          case  3 => ItemEnum.THIEF_SECRET
          case  4 => ItemEnum.VENTRILOQUIST
          case  5 => ItemEnum.DMESSAGE_SEAL
          case  6 => ItemEnum.MIRROR_SHIELD
          case  7 => ItemEnum.WEATHER_ROD
          case  8 => ItemEnum.DEATH_NOTE
          case  9 => ItemEnum.SHAMAN_CROWN
          case 10 => ItemEnum.POPULATION_CENSUS
		  case 11 => ItemEnum.MICROPHONE
        }).toString
        user.item_flags(new_item)
      }
      if ((user.item_flags.is == ItemEnum.BLESS_STAFF.toString) ||
          (user.item_flags.is == ItemEnum.BLACK_FEATHER.toString))
        user.cash(user.cash.is + 1)
    }


    votes.foreach {vote =>
      val actioner_list =  user_entrys.filter(_.id.is == vote.actioner_id.is)
      val actionee_list =  user_entrys.filter(_.id.is == vote.actionee_id.is)
      if ((actioner_list.length != 0) && (actionee_list.length != 0)) {
        val actioner = actioner_list(0)
        val actionee = actionee_list(0)
        // 投哥德法的人
        if ((actionee.current_role == RoleEnum.GODFAT) &&
//            ((actionee.has_flag(UserEntryFlagEnum.GODFAT_SPECIAL1)) ||
//             (actionee.has_flag(UserEntryFlagEnum.GODFAT_SPECIAL3))) &&
            (actioner.hasnt_flag(UserEntryFlagEnum.GODFAT_TARGETED))) {
          actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_TARGETED.toString)
          //x actioner.save
        }
		
		//投撒旦的人
		if ((actioner.get_fallenangel_special == RoleSpecialEnum.WRATH.toString) &&
            (actionee.has_flag(UserEntryFlagEnum.FALLENANGEL_WRATH_TARGETED1))) {
		  actionee.user_flags(actionee.user_flags.is.replace(UserEntryFlagEnum.FALLENANGEL_WRATH_TARGETED1.toString,UserEntryFlagEnum.FALLENANGEL_WRATH_TARGETED2.toString))
        }
        if ((actioner.get_fallenangel_special == RoleSpecialEnum.WRATH.toString) &&
            (actionee.hasnt_flag(UserEntryFlagEnum.FALLENANGEL_WRATH_TARGETED1)) &&
            (actionee.hasnt_flag(UserEntryFlagEnum.FALLENANGEL_WRATH_TARGETED2))) {
          actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.FALLENANGEL_WRATH_TARGETED1.toString)
        }

        // 橋姬閃光
        if ((((actioner.current_role == RoleEnum.FALLEN_ANGEL) &&
			   (actioner.role.is.substring(1,2) == RoleSpecialEnum.ENVY.toString)) || 
			  (actioner.subrole.is == SubroleEnum.HASHIHIME.toString)) &&
            (actionee.has_flag(UserEntryFlagEnum.LOVER))) {
          val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message("＜＜"+ JinrouUtil.encodeHtml(actionee.handle_name.is, 80) +"的身上閃閃發光＞＞").font_type("12")
          talks_for_save = talks_for_save ::: List(talk)
        }
        if ((((actionee.current_role == RoleEnum.FALLEN_ANGEL) &&
			   (actionee.role.is.substring(1,2) == RoleSpecialEnum.ENVY.toString)) || 
			  (actionee.subrole.is == SubroleEnum.HASHIHIME.toString)) &&
            (actioner.has_flag(UserEntryFlagEnum.LOVER))) {
          val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message("＜＜"+ JinrouUtil.encodeHtml(actioner.handle_name.is, 80) +"的身上閃閃發光＞＞").font_type("12")
          talks_for_save = talks_for_save ::: List(talk)

        }

      }
    }
    
    // 被投的人吊死
    if (voted_player == WaterElemental) {
      val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                    .actioner_id(0)
      sys_mes.mtype(MTypeEnum.DEATH_HANGED.toString)
      sys_mes.save

      user_entrys.filter(_.current_role == RoleEnum.ARCHMAGE).foreach { archmage =>
        archmage.user_flags(archmage.user_flags.is + UserEntryFlagEnum.WATER_ELEM_USED.toString)
        //x archmage.save()
      }
    } else {
      process_death(room, room_day, voted_player, user_entrys, MTypeEnum.DEATH_HANGED)
      
      val alchemists = user_entrys.filter(_.current_role == RoleEnum.ALCHEMIST)
      if (alchemists.length != 0) {
        val voted_essence = if (voted_player.subrole.is == SubroleEnum.WOLFSTAMP.toString)
          UserEntryFlagEnum.FIRE
        else RoleEnum.get_role(voted_player.current_role).role_side match {
          case RoomVictoryEnum.VILLAGER_WIN => UserEntryFlagEnum.WATER
          case RoomVictoryEnum.WEREWOLF_WIN => UserEntryFlagEnum.FIRE
          case RoomVictoryEnum.FOX_WIN => UserEntryFlagEnum.AIR
          case x => UserEntryFlagEnum.EARTH
        }

        alchemists.foreach { alchemist =>
          if (alchemist.has_flag(UserEntryFlagEnum.ALCHEMIST_ALTERNATE))
            alchemist.user_flags(alchemist.user_flags.is.replace(UserEntryFlagEnum.ALCHEMIST_ALTERNATE.toString,""))
          else if (alchemist.hasnt_flag(voted_essence))
            alchemist.user_flags(alchemist.user_flags.is + voted_essence.toString + UserEntryFlagEnum.ALCHEMIST_ALTERNATE.toString)
        }
      }
    }

    // 絕望 Counter 下降
    val death0s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.DEATH_0))
    death0s.foreach { suddendeath  =>
       if (suddendeath.live.is)
         process_death(room, room_day, suddendeath, user_entrys, MTypeEnum.DEATH_SUDDEN)
    }
    val death1s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.DEATH_1))
    death1s.foreach { suddendeath =>
       suddendeath.user_flags(suddendeath.user_flags.is.replace(UserEntryFlagEnum.DEATH_1.toString,UserEntryFlagEnum.DEATH_0.toString))
       //x suddendeath.save
    }
    val death2s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.DEATH_2))
    death2s.foreach { suddendeath =>
       suddendeath.user_flags(suddendeath.user_flags.is.replace(UserEntryFlagEnum.DEATH_2.toString,UserEntryFlagEnum.DEATH_1.toString))
       //x suddendeath.save
    }

    if (voted_player != WaterElemental) {
      // 如果被吊死的人是幼狼的話
      if (voted_player.current_role == RoleEnum.WOLFCUB) {
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL_NORUNNER.toString).message("＜＜幼狼被吊，人狼狂暴＞＞").font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }

	  // 如果被吊死的人是哥德法的話
      else if ((voted_player.current_role == RoleEnum.GODFAT) && 
               (voted_player.hasnt_flag(UserEntryFlagEnum.GODFAT_SPECIAL2)) &&
               (voted_player.hasnt_flag(UserEntryFlagEnum.GODFAT_SPECIAL3)) &&
               (voted_player.hasnt_flag(UserEntryFlagEnum.GODFAT_SPECIAL4))) {
        val avenger_votes = votes.filter(_.actioner_id.is == voted_player.id.is)
        if (avenger_votes.length != 0) {
          val avenger_vote = avenger_votes(0)
          val target_list  = user_entrys.filter(_.id.is == avenger_vote.actionee_id.is)
          val target =
            if (target_list.length == 0)
              user_entrys.filter(x=>(x.live.is) && (x.current_role == RoleEnum.ARCHMAGE))(0)
            else
              target_list(0)
          //if ((target.live.is) && (target.subrole.is == "")) {
          if (target.live.is) {
             // target.subrole(SubroleEnum.SUDDENDEATH.toString)
             target.user_flags(target.user_flags.is + UserEntryFlagEnum.DEATH_2.toString)
             //x target.save
          }
        }
      }

      // 如果被吊死的人是復仇者的話
      if ((room_day.weather.is != WeatherEnum.RAINY.toString) &&
	     (voted_player.subrole.is == SubroleEnum.AVENGER.toString)) {
        val avenger_votes = votes.filter(_.actioner_id.is == voted_player.id.is)
        if (avenger_votes.length != 0) {
          val avenger_vote = avenger_votes(0)
          val target_list  = user_entrys.filter(_.id.is == avenger_vote.actionee_id.is)
          if (target_list.length == 0) {
            val target = user_entrys.filter(x=>(x.live.is) && (x.current_role == RoleEnum.ARCHMAGE))(0)
            target.user_flags( target.user_flags.is + UserEntryFlagEnum.WATER_ELEM_USED.toString )
            //x target.save

            val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                         .actioner_id(0).mtype(MTypeEnum.DEATH_SUDDEN.toString)
            sys_mes.save

          } else {
            val target = target_list(0)
            if (target.live.is) {
               process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_SUDDEN)
            }
          }
        }
      }

      // 狂人單挑
      val madman_duel_votes = SystemMessage.findAll(By(SystemMessage.roomday_id, room_day.id.is),
                                                    By(SystemMessage.mtype, MTypeEnum.VOTE_MADMAN_DUEL.toString))
      //val inheriter_vote = votes.filter(_.mtype.is == MTypeEnum.VOTE_INHERITER.toString)
      madman_duel_votes.foreach { vote =>
        val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
        val target   = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)
        val actioner_votes = votes.filter(_.actioner_id.is == actioner.id.is)
        val target_votes   = votes.filter(_.actioner_id.is == target.id.is)

        if ((actioner_votes.length > 0) && (target_votes.length > 0) && (room_day.weather.is != WeatherEnum.MISTY.toString)) {
          val actioner_vote = actioner_votes(0)
          val target_vote   = target_votes(0)

          if (target_vote.vote_number.is > actioner_vote.vote_number.is)
            process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_SUDDEN)
          else
            process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_SUDDEN)
        }
      }

      // 繼承者繼承
      val inheriter_votes = SystemMessage.findAll(By(SystemMessage.roomday_id, room_day.id.is),
                                                 By(SystemMessage.mtype, MTypeEnum.VOTE_INHERITER.toString))
      //val inheriter_vote = votes.filter(_.mtype.is == MTypeEnum.VOTE_INHERITER.toString)
      inheriter_votes.foreach { vote =>
        val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
        val target   = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)
        val target_role_str = target.role.is.substring(0,1)
        val target_role_str2 =
          if ((room.has_flag(RoomFlagEnum.ROLE_AUGHUNTER)) &&
              ((target_role_str == RoleEnum.AUGURER.toString) ||
               (target_role_str == RoleEnum.HUNTER.toString)))
            RoleEnum.AUGHUNTER.toString
          else if ((target_role_str == RoleEnum.INHERITER.toString) ||
               (target_role_str == RoleEnum.SHIFTER.toString))
            RoleEnum.HERMIT.toString
          else
            target_role_str
      
        if ((actioner.live.is) && (target.id.is == voted_player.id.is)) {
          if (target_role_str2 != RoleEnum.INHERITER.toString) {
            actioner.role(target_role_str2 + actioner.role.is)
            //x actioner.save
          }

          if ((!room.has_flag(RoomFlagEnum.INHERITER_NEUTRAL))) {
            val target_role_enum = RoleEnum.get_role(target_role_str2)
            val talk_sentence =
              if (room.has_flag(RoomFlagEnum.INHERITER_REVEAL))
                "＜＜繼承者繼承成功＞＞ (" + target_role_enum.toString + ")"
              else
                "＜＜繼承者繼承成功＞＞"

            val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")

            talks_for_save = talks_for_save ::: List(talk)
          }
        }
      }
    
      // 吊到毒？
      if (voted_player.current_role == RoleEnum.POISONER) {
        val live_player = user_entrys.filter(x=>(x.live.is)&&(x.hasnt_flag(UserEntryFlagEnum.HIDED))&&
                                                             (x.get_werewolf_special != RoleSpecialEnum.RESIST.toString))
        val random_player = live_player(random.nextInt(live_player.length))
        process_death(room, room_day, random_player, user_entrys, MTypeEnum.DEATH_POISON_D)
      }

      if (voted_player.get_werewolf_special == RoleSpecialEnum.POISON.toString) {
        val live_player = user_entrys.filter(x=>(x.live.is) && (x.hasnt_flag(UserEntryFlagEnum.HIDED))
                                                            && (x.current_role != RoleEnum.WEREWOLF)
                                                            && (x.current_role != RoleEnum.WOLFCUB)
                                                            && (x.current_role != RoleEnum.MADMAN)
                                                            && (x.current_role != RoleEnum.SORCEROR))
        if (live_player.length > 0) {
          val random_player = live_player(random.nextInt(live_player.length))
          process_death(room, room_day, random_player, user_entrys, MTypeEnum.DEATH_POISON_D)
        }
      }
    }

    // 處理自投者
    //val votes_auto = votes.filter(_.vote_flags.is.indexOf(VoteFlagEnum.AUTO.toString) != -1)
    val votes_auto = votes.filter(_.vote_flags.is.indexOf(VoteFlagEnum.AUTO.toString) != -1)
    votes_auto.foreach { vote_auto =>
      val auto_player = user_entrys.filter(_.id.is == vote_auto.actioner_id.is)(0)
      if (auto_player.hasnt_flag(UserEntryFlagEnum.AUTOVOTED))  {
        auto_player.user_flags(auto_player.user_flags.is + UserEntryFlagEnum.AUTOVOTED.toString)
        //x auto_player.save
      }
      else if (auto_player.live.is) {
        process_death(room, room_day, auto_player, user_entrys, MTypeEnum.DEATH_SUDDEN)
      }
      
    }
    
    // 處理暴斃者
    if ((room_day.weather.is != WeatherEnum.RAINY.toString) && (room_day.day_no.is == 12)) {
      val live_suddendeaths = user_entrys.filter(x=>(x.subrole.is == SubroleEnum.SUDDENDEATH.toString) && (x.live.is))
      live_suddendeaths.foreach { live_suddendeath =>
        process_death(room, room_day, live_suddendeath, user_entrys, MTypeEnum.DEATH_SUDDEN)
      }
    }
    
    // 若狐全掛，背德連帶死亡
    // 若狼全掛，幼狼連帶死亡
    process_followers(room, room_day, user_entrys)

    // 計算背德票數
    if ((room.has_flag(RoomFlagEnum.BETRAYER_OPTION1)) ||
        (room.has_flag(RoomFlagEnum.BETRAYER_OPTION2)) ||
        (room.has_flag(RoomFlagEnum.BETRAYER_OPTION3))) {
      votes.foreach{ vote=>
         val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
         if ((actioner.current_role == RoleEnum.BETRAYER) && actioner.live.is) {
           actioner.action_point(actioner.action_point.is + (vote.vote_number.is /2 ))
           //x actioner.save
         }
      }
    }

    // 計算其他狐系票數
    if (room.has_flag(RoomFlagEnum.FOX_OPTION4)) {
      votes.foreach{ vote=>
         val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
         if (((actioner.current_role == RoleEnum.FOX) ||
              (actioner.has_flag(UserEntryFlagEnum.GODFAT_SPECIAL3))) &&
              actioner.live.is) {
           actioner.action_point(actioner.action_point.is + (vote.vote_number.is /2 ))
           //x actioner.save
         }
      }
    }

    // 狂人 STUN 回復
    val stunned1s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.STUNNED_1))
    stunned1s.foreach { stunned1 =>
       stunned1.user_flags(stunned1.user_flags.is.replace(UserEntryFlagEnum.STUNNED_1.toString,""))
       //x stunned1.save
    }
    val stunned2s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.STUNNED_2))
    stunned2s.foreach { stunned2 =>
       stunned2.user_flags(stunned2.user_flags.is.replace(UserEntryFlagEnum.STUNNED_2.toString,UserEntryFlagEnum.STUNNED_1.toString))
       //x stunned2.save
    }
    val stunned3s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.STUNNED_3))
    stunned3s.foreach { stunned3 =>
       stunned3.user_flags(stunned3.user_flags.is.replace(UserEntryFlagEnum.STUNNED_3.toString,UserEntryFlagEnum.STUNNED_2.toString))
       //x stunned3.save
    }

    // 回復隱士神隱狀態
    val hided_user_entrys = user_entrys.filter(_.has_flag(UserEntryFlagEnum.HIDED))
    hided_user_entrys.foreach { hided_user_entry =>
      hided_user_entry.user_flags(hided_user_entry.user_flags.replace(UserEntryFlagEnum.HIDED.toString, ""))
      //x hided_user_entry.save
    }

    //users_for_save.removeDuplicates.foreach( _.save )

    // 顯示道具訊息
    if (room.has_flag(RoomFlagEnum.ITEM_MODE)) {
      val talk_sentence =
        if (room_day.item.is.endsWith("*"))
          "＜＜道具競標成功＞＞"
        else
          "＜＜道具競標失敗＞＞"

      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    // 計算天氣變化
    val old_weather = room_day.weather.is
    var new_weather = room_day.weather.is
    if (((room.has_flag(RoomFlagEnum.WEATHER)) && (random.nextInt(10) != 0)) ||
        (old_weather == WeatherEnum.TYPHOON.toString)) {
      val weather_int = random.nextInt(5)
      new_weather = weather_int match
      {
        case 0 => WeatherEnum.SUNNY.toString
        case 1 => WeatherEnum.CLOUDY.toString
        case 2 => WeatherEnum.RAINY.toString
        case 3 => WeatherEnum.SNOWY.toString
        case 4 => WeatherEnum.MISTY.toString
        //case 5 => WeatherEnum.TYPHOON.toString
        case x => WeatherEnum.SUNNY.toString
      }

      if ((room.has_flag(RoomFlagEnum.WEATHER1)) && (random.nextInt(20) == 0) &&
          (old_weather != WeatherEnum.TYPHOON.toString) && (voted_player.current_role != RoleEnum.WOLFCUB))
        new_weather = WeatherEnum.TYPHOON.toString
    }
	
	// 清除墮落
	val fallenangels = user_entrys.filter(x =>(x.current_role == RoleEnum.FALLEN_ANGEL))
    val live_fallenangels = user_entrys.filter(x =>(x.live.is) && (x.current_role == RoleEnum.FALLEN_ANGEL))
	val fallens = user_entrys.filter(_.has_flag(UserEntryFlagEnum.FALLEN))
	if ((fallenangels.length >= 1) && (live_fallenangels.length == 0) && (fallens.length != 0)) {
	  fallens.foreach { fallen =>
          fallen.user_flags(fallen.user_flags.is.replace(UserEntryFlagEnum.FALLEN.toString,""))
	  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + fallen.handle_name +" 的周圍的黑暗消失了…＞＞")
      talk.save
      }
	}
	
	// 清除冰凍
	if (room.has_flag(RoomFlagEnum.PENGUIN_OPTION4)) {
	  val penguins = user_entrys.filter(x =>(x.current_role == RoleEnum.PENGUIN))
      val live_penguins = user_entrys.filter(x =>(x.live.is) && (x.current_role == RoleEnum.PENGUIN))
	  val iced1_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_1))
	  val iced2_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_2))
	  val iced3_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_3))
	  if ( (penguins.length >= 1) && (live_penguins.length == 0)) {
	    if (iced1_users.length != 0){
	      iced1_users.foreach { iced1_user =>
          iced1_user.user_flags(iced1_user.user_flags.is.replace(UserEntryFlagEnum.ICED_1.toString,""))
          val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + iced1_user.handle_name +" 身邊的冰晶融化了＞＞")
          talks_for_save = talks_for_save ::: List(talk)
        }
	   }
	  if (iced2_users.length != 0){
	    iced2_users.foreach { iced2_user =>
          iced2_user.user_flags(iced2_user.user_flags.is.replace(UserEntryFlagEnum.ICED_2.toString,""))
          val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + iced2_user.handle_name +" 不再散發著寒氣了＞＞")
          talks_for_save = talks_for_save ::: List(talk)
        }
	   }
	  if (iced3_users.length != 0){
	    iced3_users.foreach { iced3_user =>
          iced3_user.user_flags(iced3_user.user_flags.is.replace(UserEntryFlagEnum.ICED_3.toString,""))
          val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + iced3_user.handle_name +" 覺得溫暖些了＞＞")
          talks_for_save = talks_for_save ::: List(talk)
        }
	   }
	  }
	}
	
	//清除信仰動搖成功標記
	  val faith_successs = user_entrys.filter(x =>(x.has_flag(UserEntryFlagEnum.HERETIC_FAITH_SUCCESS)))
	  faith_successs.foreach { faith_success =>
          faith_success.user_flags(faith_success.user_flags.is.replace(UserEntryFlagEnum.HERETIC_FAITH_SUCCESS.toString,""))
      }
	
	// 補充異端鮮血
	  val live_heretics = user_entrys.filter(x=>(x.current_role == RoleEnum.HERETIC) && (x.live.is))
      val dead_user_nohps = user_entrys.filter(x =>(!x.live.is) && (x.hasnt_flag(UserEntryFlagEnum.HERETIC_HP)))
	  live_heretics.foreach { live_heretic =>
		if ( (live_heretics.length >= 1) && (dead_user_nohps.length >= 1)) {
		    live_heretic.action_point(live_heretic.action_point.is + dead_user_nohps.length)
        }
	  }
	  dead_user_nohps.foreach { dead_user_nohp =>
		if ( (live_heretics.length >= 1) && (dead_user_nohps.length >= 1)) {
		    dead_user_nohp.user_flags(dead_user_nohp.user_flags.is.toString + UserEntryFlagEnum.HERETIC_HP.toString)
        }
	  }
	  
	// 道具傳聲筒
	val item_microphones = ItemVote.findAll(By(ItemVote.roomday_id, room_day.id.is),
                                                    By(ItemVote.mtype, MTypeEnum.ITEM_MICROPHONE.toString))
    item_microphones.foreach { item_microphone =>
      val actioner = user_entrys.filter(_.id.is == item_microphone.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == item_microphone.actionee_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.MICROPHONE.toString)

        if (target.hasnt_flag(UserEntryFlagEnum.MICROPHONE))
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.MICROPHONE.toString)
      }
	  val talk_sentence =  "＜＜有人正在使用傳聲筒竊竊私語＞＞"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }


    // 儲存使用者
    user_entrys.foreach { user => user.save }
	
	// 新的一日
    val new_room_day  = RoomDay.create.room_id(room.id.is).day_no(room_day.day_no.is + 1)
                        .vote_time(1).weather(new_weather).item(generate_item(room, user_entrys).toString)
    new_room_day.save

    // 白天到晚上時不用加入 SystemMessage
    //votes.foreach { vote =>
    //  val sys_mes = SystemMessage.create.roomday_id(new_room_day.id.is)
    //                .actioner_id(vote.actioner_id.is).actionee_id(vote.actionee_id.is)
    //                .mtype(vote.mtype.is)
    //  sys_mes.save
    //}
    
    // 加入繼承者訊息
    talks_for_save.foreach { talk_for_save =>
      talk_for_save.roomday_id(new_room_day.id.is)
      talk_for_save.save
    }
    
    // 進入下一天
    /*
	val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_NIGHT.toString)
    talk.save
	*/
	val weather_string =
      if (room.has_flag(RoomFlagEnum.WEATHER))
        " (" + WeatherEnum.get_weather(new_room_day.weather.is) + ")"
      else
        ""
    val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("< < 日落、黑暗的夜晚來臨 > >" + weather_string)
    talk.save
    //room.addToRoom_days(new_day)
    //room.save(flush:true)
	
    
    // 吊到惡魔？
    if ((voted_player != WaterElemental) && (voted_player.current_role == RoleEnum.DEMON)) {
      if (voted_player.has_flag(UserEntryFlagEnum.BITED))
        // 惡魔直接獲勝
        return RoomVictoryEnum.DEMON_WIN
      else {
        // 清除凍結
        //val penguins = user_entrys.filter(_.current_role == RoleEnum.PENGUIN)
        //penguins.foreach { penguin =>
        //  penguin.action_point(0)
        //  penguin.save
        //}
      }
    }
    return RoomVictoryEnum.NONE
  }
  
  def process_night(room:Room, room_day:RoomDay, user_entrys:List[UserEntry], votes:List[Vote]) = {
    val random                           = new Random()
    //var users_for_save : List[UserEntry] = List()
    var talks_for_save : List[Talk]      = List()
    var votes_for_save : List[Vote]      = List()
    var itemvotes_for_save : List[ItemVote] = List()
    
    var item_votes = ItemVote.findAll(By(ItemVote.roomday_id, room_day.id.is))

    // ALTERNATE 回復
    val alternates  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.ALTERNATE))
    alternates.foreach { alternate =>
      alternate.user_flags(alternate.user_flags.is.replace(UserEntryFlagEnum.ALTERNATE.toString, ""))
    }
	
	// 傳聲筒解除
	val microphone_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.MICROPHONE))
	microphone_users.foreach { microphone_user =>
	  microphone_user.user_flags(microphone_user.user_flags.is.replace(UserEntryFlagEnum.MICROPHONE.toString,""))
	  //x microphone_user.save
    }
	
	// 道具模式每個生存者金錢+1
	val liveusers  =  user_entrys.filter(_.live.is)
	liveusers.foreach { liveuser =>
      liveuser.cash(liveuser.cash.is + 1)
    }

    // 妖狐指定背德
    val fox_votes = votes.filter(x => (x.mtype.is == MTypeEnum.VOTE_FOX.toString) ||
                                      (x.mtype.is == MTypeEnum.VOTE_FOX1.toString))
    fox_votes.foreach { vote =>
      val target = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)
      target.role(RoleEnum.BETRAYER.toString)
      target.action_point(1)
      //x target.save
    }

    // 模仿師模仿
    val shifter_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_SHIFTER.toString)
    shifter_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)
      val target_role_str = target.role.is.substring(0,1)
      val target_role_str2 =
        if ((room.has_flag(RoomFlagEnum.ROLE_AUGHUNTER)) &&
            ((target_role_str == RoleEnum.AUGURER.toString) ||
             (target_role_str == RoleEnum.HUNTER.toString)))
          RoleEnum.AUGHUNTER.toString
        else if ((room.has_flag(RoomFlagEnum.ROLE_PENGUIN)) &&
                 (target_role_str == RoleEnum.DEMON.toString))
          RoleEnum.PENGUIN.toString
        else if (target_role_str == RoleEnum.ARCHMAGE.toString)
          RoleEnum.VILLAGER.toString
		else if (target_role_str == RoleEnum.FALLEN_ANGEL.toString)
          RoleEnum.DEMON.toString
        else
          target_role_str
	  if (target_role_str == RoleEnum.HERETIC.toString) {
	    actioner.action_point(1)
	  }
      if (room.has_flag(RoomFlagEnum.SHIFTER_LINKS)) {
        target.user_flags(target.user_flags.is + UserEntryFlagEnum.LINKS.toString)
        //x target.save
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.LINKS.toString)
      }
      actioner.role(target_role_str2 + actioner.role.is)
      //x actioner.save

      val target_role_enum =
        RoleEnum.get_role(target_role_str2)

      val talk_sentence =
        if (room.has_flag(RoomFlagEnum.SHIFTER_REVEAL))
          "＜＜模仿師模仿成功＞＞ (" + target_role_enum.toString + ")"
        else
          "＜＜模仿師模仿成功＞＞"

      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }
    val shifter_votes2 = votes.filter(_.mtype.is == MTypeEnum.VOTE_SHIFTER2.toString)
    shifter_votes2.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
	  val target_role_str2 =
        if (room.has_flag(RoomFlagEnum.GM_PONTIFF1))
          RoleEnum.DEMON.toString
        else
          RoleEnum.PENGUIN.toString

      actioner.role(target_role_str2 + actioner.role.is)
      //x actioner.save

      val target_role_enum = RoleEnum.get_role(target_role_str2)
      val talk_sentence =
        if (room.has_flag(RoomFlagEnum.SHIFTER_REVEAL))
          "＜＜模仿師模仿成功＞＞ (" + target_role_enum.toString + ")"
        else
          "＜＜模仿師模仿成功＞＞"

      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }
	val shifter_votes3 = votes.filter(_.mtype.is == MTypeEnum.VOTE_SHIFTER3.toString)
    shifter_votes3.foreach { vote =>
	  val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      var vote_array_shifter : java.util.LinkedList[Int] = new java.util.LinkedList()
      val user_no_shifter = user_entrys.filter(x=>(x.uname.is != "dummy_boy") && (x.id.is != actioner.id.is)).map(_.user_no.is)
      user_no_shifter.foreach{i =>
        vote_array_shifter.add(i)
      }
      java.util.Collections.shuffle(vote_array_shifter)
      var vote_shifter = vote_array_shifter.removeFirst()
      var vote_shifter_users = user_entrys.filter(_.user_no.is == vote_shifter)
	  val target_role_str = vote_shifter_users(0).role.is.substring(0,1)
	  val target_role_str3 =
        if ((room.has_flag(RoomFlagEnum.ROLE_AUGHUNTER)) &&
            ((target_role_str == RoleEnum.AUGURER.toString) ||
             (target_role_str == RoleEnum.HUNTER.toString)))
          RoleEnum.AUGHUNTER.toString
        else if ((room.has_flag(RoomFlagEnum.ROLE_PENGUIN)) &&
                 (target_role_str == RoleEnum.DEMON.toString))
          RoleEnum.PENGUIN.toString
        else if (target_role_str == RoleEnum.ARCHMAGE.toString)
          RoleEnum.VILLAGER.toString
        else if (target_role_str == RoleEnum.FALLEN_ANGEL.toString)
          RoleEnum.DEMON.toString
        else
          target_role_str
		  
	  if (target_role_str == RoleEnum.HERETIC.toString) {
	    actioner.action_point(1)
	  }
	  if (room.has_flag(RoomFlagEnum.SHIFTER_LINKS)) {
        vote_shifter_users(0).user_flags(vote_shifter_users(0).user_flags.is + UserEntryFlagEnum.LINKS.toString)
        //x target.save
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.LINKS.toString)
      }
      actioner.role(target_role_str3 + actioner.role.is)
	  actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.SHIFTER_USER.toString)
	  vote_shifter_users(0).user_flags(vote_shifter_users(0).user_flags.is + UserEntryFlagEnum.SHIFTER_VOTE.toString)
      //x actioner.save

      val target_role_enum = RoleEnum.get_role(target_role_str3)

      val talk_sentence =
        if (room.has_flag(RoomFlagEnum.SHIFTER_REVEAL))
          "＜＜模仿師隨機模仿成功＞＞ (" + target_role_enum.toString + ")"
        else
          "＜＜模仿師隨機模仿成功＞＞"

      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    // 隱士技能
    val hermit_hide_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_HIDE.toString)
    hermit_hide_votes.foreach { hermit_hide_vote =>
      val actioner = user_entrys.filter(_.id.is == hermit_hide_vote.actioner_id.is)(0)
      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.HIDED.toString)
      //x actioner.save

      val target_hermit_votes = votes.filter(_.actionee_id.is == actioner.id.is)
      target_hermit_votes.foreach { target_hermit_vote =>
        target_hermit_vote.actionee_id(target_hermit_vote.actioner_id.is)
        target_hermit_vote.save
      }
    }
    val hermit_reverse_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_REVERSEVOTE.toString)
    hermit_reverse_votes.foreach { hermit_reverse_vote =>
       val actioner = user_entrys.filter(_.id.is == hermit_reverse_vote.actioner_id.is)(0)
       actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.REVERSE_USED.toString)
       //x actioner.save
    }

    // 妖狐結界
    val fox_barrier_votes = votes.filter(x => (x.mtype.is == MTypeEnum.VOTE_FOX1.toString) ||
                                              (x.mtype.is == MTypeEnum.VOTE_FOX2.toString))
    fox_barrier_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      if (actioner.live) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.FOX_SPECIAL.toString)
        //x actioner.save
      }
    }

    // 背德變化
    val betrayer_changes_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_BETRAYER_CHANGE.toString)
    betrayer_changes_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      if ((actioner.live) && (actioner.subrole.is == "") &&
          (target.current_role != RoleEnum.ARCHMAGE) &&
          (target.current_role != RoleEnum.WEREWOLF) &&
          (target.current_role != RoleEnum.WOLFCUB) &&
          (target.current_role != RoleEnum.FOX) &&
          (target.current_role != RoleEnum.DEMON) &&
          (target.current_role != RoleEnum.FALLEN_ANGEL) &&
          (target.current_role != RoleEnum.PENGUIN) &&
          (target.current_role != RoleEnum.PONTIFF) &&
		  (target.current_role != RoleEnum.HERETIC)) {
        actioner.role(target.role.is.substring(0,1))
        actioner.subrole(SubroleEnum.FOXBELIEVER.toString)
        actioner.action_point(0)
        //x actioner.save
        
        val target_role_str = target.role.is.substring(0,1)
        val target_role_enum =  RoleEnum.get_role(target_role_str)

        val talk_sentence = "＜＜背德變化成功＞＞ (" + target_role_enum.toString + ")"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_FOX.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    // 哥德法特化
    val godfat_special1_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_SPECIAL1.toString)
    godfat_special1_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL1.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法特化成功＞＞ (咒術)"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    val godfat_special2_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_SPECIAL2.toString)
    godfat_special2_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL2.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法特化成功＞＞ (方陣)"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    val godfat_special3_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_SPECIAL3.toString)
    godfat_special3_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL3.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法特化成功＞＞ (秘術)"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    val godfat_special4_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_SPECIAL4.toString)
    godfat_special4_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL4.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法特化成功＞＞ (預言)"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    // 哥德法絕望視線
    val godfat_deathgaze_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_DEATHGAZE.toString)
    godfat_deathgaze_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL_USED.toString)
      //x actioner.save

      actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.DEATH_2.toString)
      //x actionee.save
    }
	
	// 撒旦的憤怒
    val fallenangel_wrathanger_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_FALLENANGEL_WRATHANGER.toString)
    fallenangel_wrathanger_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)
      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.FALLENANGEL_HAVEWRATH.toString)
	  actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.DEATH_0.toString)
      //x actionee.save
    }

    if (room_day.day_no.is == 11) {
      val godfat_special_1s = user_entrys.filter(x => x.has_flag(UserEntryFlagEnum.GODFAT_SPECIAL1) &&
                                                      x.has_flag(UserEntryFlagEnum.GODFAT_SPECIAL_USED))
      godfat_special_1s.foreach { godfat_special =>
        godfat_special.user_flags(godfat_special.user_flags.is.replace(UserEntryFlagEnum.GODFAT_SPECIAL_USED.toString, ""))
      }
	  val wrath_have_1s = user_entrys.filter(x => x.get_fallenangel_special == RoleSpecialEnum.WRATH.toString &&
                                                      x.has_flag(UserEntryFlagEnum.FALLENANGEL_HAVEWRATH))
      wrath_have_1s.foreach { wrath_have =>
        wrath_have.user_flags(wrath_have.user_flags.is.replace(UserEntryFlagEnum.FALLENANGEL_HAVEWRATH.toString, ""))
      }
    }

    val godfat_hellword_votes = votes.filter(x=>(x.mtype.is == MTypeEnum.VOTE_GODFAT_HELLWORD.toString))
    godfat_hellword_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL2_USED.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法使用言咒＞＞"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_FOX.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    val godfat_colorspray_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_COLORSPRAY.toString)
    godfat_colorspray_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL_USED.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法使用七彩噴射＞＞"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_FOX.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    val godfat_blind_votes = votes.filter(x=>(x.mtype.is == MTypeEnum.VOTE_GODFAT_BLIND.toString) ||
                                             (x.mtype.is == MTypeEnum.VOTE_GODFAT_BLIND2.toString))
    godfat_blind_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SPECIAL2_USED.toString)
      //x actioner.save

      val talk_sentence = "＜＜哥德法使用眩光＞＞"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_FOX.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }

    val godfat_exchange_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_EXCHANGE.toString)
    godfat_exchange_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      actioner.role(RoleEnum.FOX.toString + actioner.role.is + actioner.role.is.substring(1))
      //x actioner.save

      actionee.role(RoleEnum.GODFAT.toString + actionee.role.is + actionee.role.is.substring(1))
      //x actionee.save
    }

    // 哥德法預言技能
    val godfat_necromancer_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_NECROMANCER.toString)
    godfat_necromancer_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      if (actionee.current_role == RoleEnum.NECROMANCER) { //靈能者
        actioner.action_point(actioner.action_point.is + 1)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_NECROMANCER.toString)
        actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.GODFAT_PREDICTED.toString)
      }
    }

    val godfat_hunter_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_HUNTER.toString)
    godfat_hunter_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      if (actionee.current_role == RoleEnum.HUNTER) { //獵人
        actioner.action_point(actioner.action_point.is + 1)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_HUNTER.toString)
        actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.GODFAT_PREDICTED.toString)
      }
    }

    val godfat_herbalist_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_HERBALIST.toString)
    godfat_herbalist_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      if ((actionee.current_role == RoleEnum.HERBALIST) ||
          (actionee.current_role == RoleEnum.ALCHEMIST)) { //藥師和鍊金術士
        actioner.action_point(actioner.action_point.is + 1)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_HERBALIST.toString)
        actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.GODFAT_PREDICTED.toString)
      }
    }

    val godfat_poisoner_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_POISONER.toString)
    godfat_poisoner_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      if (actionee.current_role == RoleEnum.POISONER) { //埋毒者
        actioner.action_point(actioner.action_point.is + 1)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_POISONER.toString)
        actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.GODFAT_PREDICTED.toString)
      }
    }

    val godfat_scholar_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_GODFAT_SCHOLAR.toString)
    godfat_scholar_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val actionee = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)

      if (actionee.current_role == RoleEnum.SCHOLAR) { //學者
        actioner.action_point(actioner.action_point.is + 1)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.GODFAT_SCHOLAR.toString)
        actionee.user_flags(actionee.user_flags.is + UserEntryFlagEnum.GODFAT_PREDICTED.toString)
      }
    }

    // 道具邱比特之箭
    val item_cubic_arrows = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_CUBIC_ARROW.toString)
    item_cubic_arrows.foreach { item_cubic_arrow =>
      val actioner = user_entrys.filter(_.id.is == item_cubic_arrow.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == item_cubic_arrow.actionee_id.is)(0)
      if (actioner.live.is && target.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.LOVER.toString)
        target.user_flags(target.user_flags.is + UserEntryFlagEnum.LOVER.toString)
		
		var total_lover = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LOVER)))

        val talk_sentence =  "＜＜村莊的某處突然冒出閃光＞＞(戀人總數：" + total_lover.length.toString + ")"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    // 道具盜賊極意
    val item_thief_secrets = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_THIEF_SECRET.toString)
    item_thief_secrets.foreach { item_thief_secret =>
      val actioner = user_entrys.filter(_.id.is == item_thief_secret.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == item_thief_secret.actionee_id.is)(0)
      if (actioner.live.is) {
        item_votes = item_votes.filter(_.actioner_id.is != target.id.is)

        actioner.item_flags(target.item_flags.is)
        target.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		val talk_sentence =  "＜＜昨晚似乎有人試著竊取他人的道具＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    // 道具鏡盾
    val item_mirror_shields = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_MIRROR_SHIELD.toString)
    item_mirror_shields.foreach { item_mirror_shield =>
      val actioner = user_entrys.filter(_.id.is == item_mirror_shield.actioner_id.is)(0)
       actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)

      val target_mirror_shield_votes = votes.filter(_.actionee_id.is == actioner.id.is)
      target_mirror_shield_votes.foreach { target_mirror_shield_vote =>
        target_mirror_shield_vote.actionee_id(target_mirror_shield_vote.actioner_id.is)
        target_mirror_shield_vote.save
      }
	  val talk_sentence =  "＜＜昨晚似乎有人使用了鏡盾卷軸＞＞"
      val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
      talks_for_save = talks_for_save ::: List(talk)
    }
	// 路西法的驕傲
	val fallenangel_prideprouds = votes.filter(_.mtype.is == MTypeEnum.VOTE_FALLENANGEL_PRIDEPROUD.toString)
	fallenangel_prideprouds.foreach { fallenangel_prideproud =>
      val actioner = user_entrys.filter(_.id.is == fallenangel_prideproud.actioner_id.is)(0)
	    actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.FALLENANGEL_HAVEUSED.toString)
	  
      val target_mirror_shield_votes = votes.filter(_.actionee_id.is == actioner.id.is)
      target_mirror_shield_votes.foreach { target_mirror_shield_vote =>
        target_mirror_shield_vote.actionee_id(target_mirror_shield_vote.actioner_id.is)
        target_mirror_shield_vote.save
      }
    }

    // 學者能力
    val scholar_analyzes = votes.filter(_.mtype.is == MTypeEnum.VOTE_SCHOLAR_ANALYZE.toString)
    scholar_analyzes.foreach { scholar_analyze =>
      val actioner = user_entrys.filter(_.id.is == scholar_analyze.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.ANALYZED.toString)
        //x actioner.save
      }
    }
    val scholar_reports = votes.filter(_.mtype.is == MTypeEnum.VOTE_SCHOLAR_REPORT.toString)
    scholar_reports.foreach { scholar_report =>
      val actioner = user_entrys.filter(_.id.is == scholar_report.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.REPORTED.toString)
        //x actioner.save
      }
    }

	//異端惡魔附體
	val heretic_possesseds = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERETIC_POSSESSED.toString)
	heretic_possesseds.foreach { heretic_possessed =>
	  val actioner = user_entrys.filter(_.id.is == heretic_possessed.actioner_id.is)(0)
	  
	  if (actioner.live.is) {
	    actioner.action_point(actioner.action_point.is - 3)
      }
	}
	
	//異端惡魔轉生
	val heretic_reincarnateds = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERETIC_REINCARNATED.toString)
	heretic_reincarnateds.foreach { heretic_reincarnated =>
	  val actioner = user_entrys.filter(_.id.is == heretic_reincarnated.actioner_id.is)(0)
	  
	  if (actioner.live.is) {
	    actioner.action_point(5)
		actioner.role(RoleEnum.DEMON.toString + actioner.role.is)
      }
	}
	
	//派對進行中
	val party1s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.HERETIC_PARTY_1))
    party1s.foreach { party1 =>
        party1.user_flags(party1.user_flags.is.replace(UserEntryFlagEnum.HERETIC_PARTY_1.toString, ""))
    }
    val party2s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.HERETIC_PARTY_2))
    party2s.foreach { party2 =>
        party2.user_flags(party2.user_flags.is.replace(UserEntryFlagEnum.HERETIC_PARTY_2.toString,UserEntryFlagEnum.HERETIC_PARTY_1.toString))
    }

    // 惡魔能力
    val demon_curses = votes.filter(x=>(x.mtype.is == MTypeEnum.VOTE_DEMON_CURSE.toString) ||
                                       (x.mtype.is == MTypeEnum.VOTE_DEMON_CURSE2.toString))
    demon_curses.foreach { demon_curse =>
      val actioner = user_entrys.filter(_.id.is == demon_curse.actioner_id.is)(0)
      if (actioner.live.is) {
        if ((demon_curse.actionee_id.is != 0) && (demon_curse.actionee_id.is != demon_curse.actioner_id.is))
          actioner.action_point(actioner.action_point.is + 1)
        else
          actioner.action_point(actioner.action_point.is + 2)
        //actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.CURSE_USED.toString)
        //x actioner.save
      }
    }

    val demon_chaoss = votes.filter(_.mtype.is == MTypeEnum.VOTE_DEMON_CHAOS.toString)
    demon_chaoss.foreach { demon_chaos =>
      val actioner = user_entrys.filter(_.id.is == demon_chaos.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.action_point(actioner.action_point.is + 2)
        //x actioner.save
      }
    }

    val demon_dominates = votes.filter(_.mtype.is == MTypeEnum.VOTE_DEMON_DOMINATE.toString)
    demon_dominates.foreach { demon_dominate =>
      val actioner = user_entrys.filter(_.id.is == demon_dominate.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.action_point(actioner.action_point.is + 1)
        //x actioner.save
      }
    }

    val demon_vortexes = votes.filter(_.mtype.is == MTypeEnum.VOTE_DEMON_VORTEX.toString)
    demon_vortexes.foreach { demon_vortex =>
      val actioner = user_entrys.filter(_.id.is == demon_vortex.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.action_point(actioner.action_point.is + 3)
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.VORTEX_USED.toString)
        //x actioner.save
      }
    }
	// 放棄儀式
	val demon_giveups = votes.filter(_.mtype.is == MTypeEnum.VOTE_DEMON_GIVEUP.toString)
    demon_giveups.foreach { demon_giveup =>
      val actioner = user_entrys.filter(_.id.is == demon_giveup.actioner_id.is)(0)
	  val pontiff    = user_entrys.filter(x=>(x.current_role == RoleEnum.PONTIFF))
      if (actioner.live.is) {
          if (actioner.role.is.toString.length > 1) {
            if (room.has_flag(RoomFlagEnum.FALLENANGEL_OPTION1)) {
              if((room.has_flag(RoomFlagEnum.ITEM_MODE)) || (pontiff.length != 0)) {
                val role_special_list = RoleSpecialEnum.ROLESPECIAL_FALLENANGEL.keys.toList
                val role_special = role_special_list(random.nextInt(role_special_list.length))
                actioner.role(RoleEnum.FALLEN_ANGEL.toString + role_special + actioner.role.is.toString.substring(1))
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.BITED.toString, ""))
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.RELIGION.toString, ""))
              } else {
                val role_special_list = RoleSpecialEnum.ROLESPECIAL_FALLENANGEL_NOGREED.keys.toList
                val role_special = role_special_list(random.nextInt(role_special_list.length))
                actioner.role(RoleEnum.FALLEN_ANGEL.toString + role_special + actioner.role.is.toString.substring(1))
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.BITED.toString, ""))
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.RELIGION.toString, ""))
			  }
            } else {
              actioner.role(RoleEnum.FALLEN_ANGEL.toString + actioner.role.is + actioner.role.is.toString.substring(1))
              actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.BITED.toString, ""))
              actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.RELIGION.toString, ""))
			}
          } else {
            if (room.has_flag(RoomFlagEnum.FALLENANGEL_OPTION1)) {
			  if((room.has_flag(RoomFlagEnum.ITEM_MODE)) || (pontiff.length != 0)) {
                val role_special_list = RoleSpecialEnum.ROLESPECIAL_FALLENANGEL.keys.toList
                val role_special = role_special_list(random.nextInt(role_special_list.length))
                actioner.role(RoleEnum.FALLEN_ANGEL.toString + role_special + actioner.role.is)
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.BITED.toString, ""))
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.RELIGION.toString, ""))
			  } else {
                val role_special_list = RoleSpecialEnum.ROLESPECIAL_FALLENANGEL_NOGREED.keys.toList
                val role_special = role_special_list(random.nextInt(role_special_list.length))
                actioner.role(RoleEnum.FALLEN_ANGEL.toString + role_special + actioner.role.is)
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.BITED.toString, ""))
                actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.RELIGION.toString, ""))
			  }
            } else {
              actioner.role(RoleEnum.FALLEN_ANGEL.toString + actioner.role.is)
              actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.BITED.toString, ""))
              actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.RELIGION.toString, ""))
			}
		  }
        //x actioner.save
		val actioner_role_special =  actioner.get_fallenangel_special
		if (actioner_role_special == RoleSpecialEnum.PRIDE.toString) {
		  val talk_sentence = "＜＜傲慢的 路西法 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		} else if (actioner_role_special == RoleSpecialEnum.ENVY.toString) {
		  val talk_sentence = "＜＜妒忌的 利維坦 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		} else if (actioner_role_special == RoleSpecialEnum.WRATH.toString) {
		  val talk_sentence = "＜＜暴怒的 撒旦 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		} else if (actioner_role_special == RoleSpecialEnum.LUST.toString) {
		  val talk_sentence = "＜＜淫慾的 莉莉絲 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		} else if (actioner_role_special == RoleSpecialEnum.SLOTH.toString) {
		  val talk_sentence = "＜＜怠惰的 貝利爾 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		} else if (actioner_role_special == RoleSpecialEnum.GREED.toString) {
		  val talk_sentence = "＜＜貪婪的 瑪門 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		} else if (actioner_role_special == RoleSpecialEnum.GLUTTONY.toString) {
		  val talk_sentence = "＜＜暴食的 別西卜 出現了＞＞"
		  val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
		}
      }
    }


    // 背德偽裝能力
    val betrayer_disguises = votes.filter(_.mtype.is == MTypeEnum.VOTE_BETRAYER_DISGUISE.toString)
    betrayer_disguises.foreach { betrayer_disguise =>
      val actioner = user_entrys.filter(_.id.is == betrayer_disguise.actioner_id.is)(0)
      if (actioner.live.is) {
        if ((!room.has_flag(RoomFlagEnum.CLERIC_OPTION2)) || (actioner.current_role == RoleEnum.FOX))
          actioner.action_point(actioner.action_point.is - 3)
        else
          actioner.action_point(actioner.action_point.is - 2)
        actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.CARD_HERMIT.toString, ""))
        //x actioner.save
      }
    }

    // 狼人咬的一系列處理
    var bite_successful = false
    val last_day    = RoomDay.findAll(By(RoomDay.room_id, room.id.is), By(RoomDay.day_no, room_day.day_no.is - 1))(0)
    val last_hanged_vote = SystemMessage.findAll(By(SystemMessage.roomday_id, last_day.id.is),
                                                By(SystemMessage.mtype, MTypeEnum.DEATH_HANGED.toString))
    var last_hanged : UserEntry = null
    if (last_hanged_vote.length != 0) {
      val last_hanged_list = user_entrys.filter(_.id.is == last_hanged_vote(0).actioner_id.is)
      if (last_hanged_list.length != 0) 
        last_hanged = last_hanged_list(0)
    }

    val werewolf_votes  = votes.filter(_.mtype.is == MTypeEnum.VOTE_WEREWOLF.toString)
    var werewolf_biter  = user_entrys.filter(_.id.is == werewolf_votes(0).actioner_id.is)(0)
    var werewolf_target = user_entrys.filter(_.id.is == werewolf_votes(0).actionee_id.is)(0)
    val demon_victims = votes.filter(x=>(x.mtype.is == MTypeEnum.VOTE_DEMON_CHAOS.toString) &&
                                        (x.actioner_id.is != werewolf_target.id.is) &&
                                        ((x.actionee_id.is == werewolf_biter.id.is) ||
                                         (x.actionee_id.is == werewolf_target.id.is)))
	val heretic_victims = votes.filter(x=>(x.mtype.is == MTypeEnum.VOTE_HERETIC_POSSESSED.toString) &&
                                        ((x.actioner_id.is == werewolf_biter.id.is) ||
                                         (x.actioner_id.is == werewolf_target.id.is)))

    if (demon_victims.length != 0) {
      val demon_victimer = user_entrys.filter(_.id.is == demon_victims(0).actioner_id.is)(0)
      val demon_victimee = user_entrys.filter(_.id.is == demon_victims(0).actioner_id.is)(0)

      //if ((werewolf_votes(0).actionee_id.is == demon_victimee.id.is) ||
      //    (werewolf_target.id.is == demon_victimee.id.is)) {
        // 惡魔目標轉換
        werewolf_votes(0).actionee_id(demon_victimer.id.is)
        werewolf_votes(0).vote_flags(werewolf_votes(0).vote_flags.is + VoteFlagEnum.VICTIM.toString)
        werewolf_votes(0).save

        // 轉換成功視同詛咒被用掉
        //demon_victimer.user_flags(demon_victimer.user_flags.is + UserEntryFlagEnum.CURSE_USED.toString)
        demon_victimer.user_flags(demon_victimer.user_flags.is.replace(UserEntryFlagEnum.CARD_TOWER.toString, ""))
        //x demon_victimer.save

        werewolf_target = user_entrys.filter(_.id.is == demon_victimer.id.is)(0)
      //}

    }
	//異端目標轉換
	if (heretic_victims.length != 0) {
      val heretic_victimer = user_entrys.filter(_.id.is == heretic_victims(0).actioner_id.is)(0)
      //val heretic_victimee = user_entrys.filter(_.id.is == demon_victims(0).actioner_id.is)(0)
	  val live_demon = user_entrys.filter(x=>(x.current_role == RoleEnum.DEMON) && (x.live.is) && (x.role.is.length == 1))

      //if ((werewolf_votes(0).actionee_id.is == demon_victimee.id.is) ||
      //    (werewolf_target.id.is == demon_victimee.id.is)) {
        // 惡魔目標轉換
        werewolf_votes(0).actionee_id(live_demon(0).id.is)
        werewolf_votes(0).vote_flags(werewolf_votes(0).vote_flags.is + VoteFlagEnum.VICTIM.toString)
        werewolf_votes(0).save

        // 轉換成功視同詛咒被用掉
        //demon_victimer.user_flags(demon_victimer.user_flags.is + UserEntryFlagEnum.CURSE_USED.toString)
        live_demon(0).user_flags(live_demon(0).user_flags.is.replace(UserEntryFlagEnum.CARD_TOWER.toString, ""))
        //x demon_victimer.save

        werewolf_target = user_entrys.filter(_.id.is == live_demon(0).id.is)(0)
      //}

    }

    val archmage_dispell_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_ARCHMAGE_DISPELL.toString)
    val hunter_votes             = votes.filter(_.mtype.is == MTypeEnum.VOTE_HUNTER.toString)
    hunter_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      if (actioner.current_role == RoleEnum.CARDMASTER) {
        actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.CARD_CHARIOT.toString, ""))
        //x actioner.save
      }
    }

    val herbalist_elixir_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERBALIST_ELIXIR.toString)
    val alchemist_elixir_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_ALCHEMIST_ELIXIR.toString)
    val cleric_sancture_votes    = votes.filter(_.mtype.is == MTypeEnum.VOTE_CLERIC_SANCTURE.toString)
    val runner_votes             = votes.filter(_.mtype.is == MTypeEnum.VOTE_RUNNER.toString)
    //val runners                  = runner_votes.map{x=>user_entrys.filter(_.id.is == x.actioner_id.is)(0).id.is}
    val runners                  = runner_votes.map{x=> x.actioner_id.is}

    // 是否逃亡者死亡
    runner_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == vote.actionee_id.is)(0)
      
      var runner_death = false
	  target.user_flags( target.user_flags.is + UserEntryFlagEnum.RUNNER.toString)
      if (vote.actionee_id.is == werewolf_votes(0).actionee_id.is) 
        runner_death = true
      if (target.current_role == RoleEnum.WOLFCUB)
        runner_death = true
      if (target.subrole.is == SubroleEnum.WOLFSTAMP.toString)
        runner_death = true
      val fox_barriers = fox_barrier_votes.filter(_.actioner_id.is == target.id.is)
      if (fox_barriers.length != 0)
        runner_death = true
      if ((target.current_role == RoleEnum.WEREWOLF) &&
          (target.get_werewolf_special != RoleSpecialEnum.WHITE.toString) &&
          ((room.room_flags.is.indexOf(RoomFlagEnum.RUNNER_OPTION1.toString) == -1) || // 處理逃亡者選項一
           (target.id.is != werewolf_votes(0).actioner_id.is)))
        runner_death = true
      if ( ((target.current_role == RoleEnum.SORCEROR) ||
		   (target.current_role == RoleEnum.GODFAT)) && 
           (room.has_flag(RoomFlagEnum.RUNNER_OPTION4)) &&
		   (actioner.current_role == RoleEnum.RUNNER))  //處理逃亡者選項四，巫哥，只對逃亡有效
        runner_death = true
        hunter_votes.foreach { hunter_vote =>
          if ((room.has_flag(RoomFlagEnum.RUNNER_OPTION2)) && // 處理逃亡者選項二
              (target.id.is == hunter_vote.actionee_id.is))
            runner_death = true
        }
      
      if (runner_death) {
        process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_RUNNER)
      }
    }
 
  
    // 判斷狼人咬的結果，順序 1.獵人守 2.治療藥 3.聖域術 4.咬到
    //val werewolf_power  = (werewolf_votes(0).vote_flags.is.indexOf(VoteFlagEnum.POWER.toString) != -1)
    val live_werewolfs  = user_entrys.filter(x => (x.live.is) && (x.current_role == RoleEnum.WEREWOLF))
    val werewolf_power  = (((last_hanged != null) && (last_hanged.current_role == RoleEnum.WOLFCUB)) ||
      ((live_werewolfs.length == 1) && (live_werewolfs(0).get_werewolf_special == RoleSpecialEnum.TEN.toString)))
    if (werewolf_power) {
      werewolf_votes(0).vote_flags(werewolf_votes(0).vote_flags.is + VoteFlagEnum.POWER.toString)
      werewolf_votes(0).save
    }

    //
    //println("werewolf target : " + werewolf_target.handle_name.is)
    
    //if ((werewolf_target.current_role != RoleEnum.RUNNER) || (werewolf_target.test_memoryloss(room, room_day, user_entrys)) ||
    //    (werewolf_target.test_fake(room_day)) || (room_day.day_no.is == 1)){
    if (!runners.contains(werewolf_target.id.is)) {
      if ((hunter_votes.filter(_.actionee_id.is == werewolf_target.id.is).length != 0) && (!werewolf_power)) {} // 1. 獵人守
      else if (archmage_dispell_votes.filter(_.actionee_id.is == werewolf_biter.id.is).length != 0) {}          // 2. 大魔導解除法術
      else if ((werewolf_target.current_role == RoleEnum.WOLFCUB) || (werewolf_target.current_role == RoleEnum.WEREWOLF)) {}                                           // 3. 咬到幼狼
      else if ((werewolf_target.current_role == RoleEnum.FOX) && (!werewolf_power)) {}                          // 4. 咬到狐
      else if ((werewolf_target.current_role == RoleEnum.PENGUIN) && (!werewolf_power) &&
               (room.has_flag(RoomFlagEnum.PENGUIN_OPTION2))) {}                                                // 4.1 咬到企鵝
      else if ((werewolf_target.current_role == RoleEnum.DEMON) && (!werewolf_power)) {                         // 5. 咬到惡魔
        werewolf_target.action_point(werewolf_target.action_point.is + 3)
        if (werewolf_target.hasnt_flag(UserEntryFlagEnum.BITED)) {
          werewolf_target.user_flags( werewolf_target.user_flags.is + UserEntryFlagEnum.BITED.toString )
        }
        //x werewolf_target.save
      }
      else if ((werewolf_target.current_role == RoleEnum.ARCHMAGE) &&
               (werewolf_target.hasnt_flag(UserEntryFlagEnum.WATER_ELEM_USED)) &&
               (!werewolf_power)) {                         // 6. 咬到水元素
        werewolf_target.user_flags( werewolf_target.user_flags.is + UserEntryFlagEnum.WATER_ELEM_USED.toString )
        //x werewolf_target.save

        val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                    .actioner_id(0).mtype(MTypeEnum.DEATH_EATEN.toString)
        sys_mes.save
      }
      else  {
	    //val heretic_possesseds_success = heretic_possesseds.filter(_.actioner_id.is == werewolf_target.id.is)
		val herbalist_elixirs = herbalist_elixir_votes.filter(_.actionee_id.is == werewolf_target.id.is)
        val alchemist_elixirs = alchemist_elixir_votes.filter(_.actionee_id.is == werewolf_target.id.is)
		/*
	    if ((heretic_possesseds_success.length != 0) && (!werewolf_power)) {                         // 6. 咬到惡魔附體
	      val live_demon = user_entrys.filter(x=>(x.current_role == RoleEnum.DEMON) && (x.live.is))
          werewolf_target.id.is == live_demon(0).id.is
		  werewolf_target.action_point(werewolf_target.action_point.is + 3)
        if (werewolf_target.hasnt_flag(UserEntryFlagEnum.BITED)) {
          werewolf_target.user_flags( werewolf_target.user_flags.is + UserEntryFlagEnum.BITED.toString )
        }
          //x werewolf_target.save
        }
        else */if ((herbalist_elixirs.length != 0) && (!werewolf_power)) { // 7.治療藥，只算用掉一個
          val herbalist_elixir = herbalist_elixirs(0)
          val actioner = user_entrys.filter(_.id.is == herbalist_elixir.actioner_id.is)(0)
          actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.ELIXIR_USED.toString)
          //x actioner.save
        }
        else if ((alchemist_elixirs.length != 0) && (!werewolf_power)) { // 7.鍊金治療藥，只算用掉一個
          val alchemist_elixir = alchemist_elixirs(0)
          val actioner = user_entrys.filter(_.id.is == alchemist_elixir.actioner_id.is)(0)
          actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.WATER.toString,"").replace(UserEntryFlagEnum.AIR.toString,""))
          //x actioner.save
        }
        else if (room_day.weather.is == WeatherEnum.TYPHOON.toString) {}        //7.1 颱天算咬失敗
        else {
          //val cleric_sanctures = cleric_sancture_votes.filter(_.actionee_id.is == werewolf_target.id.is)
          if ((cleric_sancture_votes.length != 0) && (!werewolf_power)) { // 8.聖域術，牧師同時放兩個都會掛
            cleric_sancture_votes.foreach { cleric_sancture =>
              val actioner = user_entrys.filter(_.id.is == cleric_sancture.actioner_id.is)(0)
              process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_CLERIC)
            } 
          } else { // 9.咬到了
            bite_successful = true
            process_death(room, room_day, werewolf_target, user_entrys, MTypeEnum.DEATH_EATEN)

            // 補充狂巫法力
            val live_sorcerors = user_entrys.filter(x=>(x.current_role == RoleEnum.SORCEROR) && (x.live.is))
            live_sorcerors.foreach { live_sorceror =>
              live_sorceror.action_point(Math.min(10, live_sorceror.action_point.is + 2))
              //x live_sorceror.save
            }

            // 咬到獵人？
            if ((werewolf_target.current_role == RoleEnum.HUNTER) &&
                (room.has_flag(RoomFlagEnum.HUNTER_OPTION1))) {
              val werewolf_target_vote = hunter_votes.filter(_.actioner_id.is == werewolf_target.id.is)
              if (werewolf_target_vote.length != 0) {
                 val hunter_target = user_entrys.filter(_.id.is == werewolf_target_vote(0).actionee_id.is)(0)
                 process_death(room, room_day, hunter_target, user_entrys, MTypeEnum.DEATH_HUNTER_KILL)
              }
            }
            
            // 如果狼人不幸咬到毒            
            if (werewolf_target.current_role == RoleEnum.POISONER) {
			  val theater_wolf = user_entrys.filter(x=>(x.current_role == RoleEnum.WEREWOLF) && (x.has_flag(UserEntryFlagEnum.THEATER)) && (x.live.is))
			  if (theater_wolf.length != 0) {
			    val random_werewolf = theater_wolf(random.nextInt(theater_wolf.length))
			    process_death(room, room_day, random_werewolf, user_entrys, MTypeEnum.DEATH_POISON_N)
			  } else if (werewolf_biter.get_werewolf_special != RoleSpecialEnum.RESIST.toString) {
                //get_werewolf_special != RoleSpecialEnum.WHITE.toString
                //val live_werewolf   = user_entrys.filter(x=>(x.current_role == RoleEnum.WEREWOLF) && (x.live.is))
                val random_werewolf = live_werewolfs(random.nextInt(live_werewolfs.length))
                process_death(room, room_day, random_werewolf, user_entrys, MTypeEnum.DEATH_POISON_N)
              }
            }
          }
        
        }
      }
    }

    if ((!bite_successful) && (room.has_flag(RoomFlagEnum.MADMAN_STUN)) &&
        (room_day.weather.is != WeatherEnum.TYPHOON.toString)) {
      // 補充狂人怒氣
      val live_madmans = user_entrys.filter(x=>(x.current_role == RoleEnum.MADMAN) && (x.live.is))
      live_madmans.foreach { live_madman =>
        live_madman.action_point(Math.min(10, live_madman.action_point.is + 1))
        //x live_madman.save
      }
    }

    val wolfcub_votes  = votes.filter(_.mtype.is == MTypeEnum.VOTE_WOLFCUB.toString)
    wolfcub_votes.foreach { wolfcub_vote =>
      val wolfcub_biter  = user_entrys.filter(_.id.is == wolfcub_vote.actioner_id.is)(0)
      val wolfcub_target = user_entrys.filter(_.id.is == wolfcub_vote.actionee_id.is)(0)

      //if ((wolfcub_target.current_role != RoleEnum.RUNNER) || (wolfcub_target.test_memoryloss(room, room_day, user_entrys)) ||
      //    (wolfcub_target.test_fake(room_day)) || (room_day.day_no.is == 1)){
      if (!runners.contains(wolfcub_target.id.is)) {
        if ((hunter_votes.filter(_.actionee_id.is == wolfcub_target.id.is).length != 0)) {} // 1. 獵人守
        else if (archmage_dispell_votes.filter(_.actionee_id.is == wolfcub_biter.id.is).length != 0) {} // 2. 大魔導解除法術
        else if ((wolfcub_target.current_role == RoleEnum.WOLFCUB) || (wolfcub_target.current_role == RoleEnum.WEREWOLF)) {}                                           // 3. 咬到幼狼
        else if ((wolfcub_target.current_role == RoleEnum.FOX) ) {}  // 4. 咬到狐
        else if ((wolfcub_target.current_role == RoleEnum.PENGUIN) &&
                 (room.has_flag(RoomFlagEnum.PENGUIN_OPTION2))) {}   // 4.1 咬到企鵝
        else if ((wolfcub_target.current_role == RoleEnum.DEMON)) {} // 5. 咬到惡魔

        // 這幼狼咬水元素新加上去
        else if ((wolfcub_target.current_role == RoleEnum.ARCHMAGE) &&
               (wolfcub_target.hasnt_flag(UserEntryFlagEnum.WATER_ELEM_USED))) { // 6. 咬到水元素
          wolfcub_target.user_flags( wolfcub_target.user_flags.is + UserEntryFlagEnum.WATER_ELEM_USED.toString )
          //x wolfcub_target.save

          val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                        .actioner_id(0).mtype(MTypeEnum.DEATH_EATEN.toString)
          sys_mes.save
        }

        else  {
          val herbalist_elixirs = herbalist_elixir_votes.filter(_.actionee_id.is == wolfcub_target.id.is)
          val alchemist_elixirs = alchemist_elixir_votes.filter(_.actionee_id.is == wolfcub_target.id.is)
          if (herbalist_elixirs.length != 0) { // 7.治療藥，只算用掉一個
            val herbalist_elixir = herbalist_elixirs(0)
            val actioner = user_entrys.filter(_.id.is == herbalist_elixir.actioner_id.is)(0)
            if (actioner.hasnt_flag(UserEntryFlagEnum.ELIXIR_USED)) {
              actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.ELIXIR_USED.toString)
              //x actioner.save
            }
          }
          else if (alchemist_elixirs.length != 0) { // 7.鍊金治療藥，只算用掉一個
            val alchemist_elixir = alchemist_elixirs(0)
            val actioner = user_entrys.filter(_.id.is == alchemist_elixir.actioner_id.is)(0)
            actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.WATER.toString,"").replace(UserEntryFlagEnum.AIR.toString,""))
            //x actioner.save
          }
          else {
            //val cleric_sanctures = cleric_sancture_votes.filter(_.actionee_id.is == werewolf_target.id.is)
            if (cleric_sancture_votes.length != 0) { // 8.聖域術，牧師同時放兩個都會掛
              cleric_sancture_votes.foreach { cleric_sancture =>
                val actioner = user_entrys.filter(_.id.is == cleric_sancture.actioner_id.is)(0)
                process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_CLERIC)
              }
            } else { // 9.咬到了
              bite_successful = true
              process_death(room, room_day, wolfcub_target, user_entrys, MTypeEnum.DEATH_WOLFCUB_EATEN)

              // 如果狼人不幸咬到毒
              if (wolfcub_target.current_role == RoleEnum.POISONER) {
                process_death(room, room_day, wolfcub_biter, user_entrys, MTypeEnum.DEATH_POISON_N)
              }
            }
          }
        }
      }
    }

    
    val augurer_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_AUGURER.toString)
    // 占卜師占到狐？
    augurer_votes.foreach { augurer_vote =>
      val actioner = user_entrys.filter(_.id.is == augurer_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == augurer_vote.actionee_id.is)(0)
      
      if ((actioner.live.is) && (!actioner.test_fake(room_day))) {
        if (target.current_role == RoleEnum.FOX) {
          val fox_barriers = fox_barrier_votes.filter(_.actioner_id.is == target.id.is)
          if (fox_barriers.length == 0)
            process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_FOX)
        } else if ((target.current_role == RoleEnum.GODFAT) ||
                   (target.get_werewolf_special == RoleSpecialEnum.CONJURE.toString)){
          process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_GODFAT)
        } else if (target.current_role == RoleEnum.DEMON) {
          target.action_point(target.action_point.is + 3)
          //x target.save
        } else if (target.subrole.is == SubroleEnum.ALPHAWOLF.toString) {
          if (target.hasnt_flag(UserEntryFlagEnum.AUGURED)) {
            augurer_vote.vote_flags(augurer_vote.vote_flags.is + VoteFlagEnum.FAKE.toString)
            augurer_vote.save

            target.user_flags(target.user_flags.is + UserEntryFlagEnum.AUGURED.toString)
            //x target.save
          }
        }

        if (actioner.current_role == RoleEnum.CARDMASTER) {
          actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.CARD_MAGICIAN.toString, ""))
          //x actioner.save
        }
      }
    }
	
	// 異端血祭
    val heretic_blood_sacrifices = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERETIC_BLOOD_SACRIFICE.toString)
    heretic_blood_sacrifices.foreach { heretic_blood_sacrifice =>
      val actioner = user_entrys.filter(_.id.is == heretic_blood_sacrifice.actioner_id.is)(0)
	  val live_demon = user_entrys.filter(x=>(x.current_role == RoleEnum.DEMON) && (x.live.is) && (x.role.is.length == 1))
	  //val demon_deity = user_entrys.filter(_.id.is == live_demon(0))
      if (actioner.live.is) {
          actioner.action_point(actioner.action_point.is - 1)
		   if (live_demon.length > 0) 
		      live_demon(0).action_point(live_demon(0).action_point.is - 1)
	  }
    }
	
	//異端信仰動搖
	val heretic_destruction_faiths = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERETIC_DESTRUCTION_FAITH.toString)
	heretic_destruction_faiths.foreach { heretic_destruction_faith =>
	  val actioner = user_entrys.filter(_.id.is == heretic_destruction_faith.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == heretic_destruction_faith.actionee_id.is)(0)
	  
	  if (actioner.live.is) {
        if (target.current_role == RoleEnum.PONTIFF) {
        } else if (target.current_role == RoleEnum.CLERIC) {
        } else if (target.subrole.is == SubroleEnum.SUBPONTIFF.toString) {
        } else if (target.has_flag(UserEntryFlagEnum.RELIGION)) {
		    target.user_flags(target.user_flags.is + UserEntryFlagEnum.HERETIC_FAITH_SUCCESS.toString)
			target.user_flags(target.user_flags.is + UserEntryFlagEnum.HERETIC_FAITH_SHAKE.toString)
			actioner.action_point(actioner.action_point.is + 1)
        }
      }
	  
	  if ((actioner.live.is) && (room.has_flag(RoomFlagEnum.HERETIC_OPTION1))) {
	    if ((actioner.hasnt_flag(UserEntryFlagEnum.HERETIC_DESTRUCTION_1)) &&
		(actioner.hasnt_flag(UserEntryFlagEnum.HERETIC_DESTRUCTION_2)))
		  actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.HERETIC_DESTRUCTION_1.toString)
		else if(actioner.has_flag(UserEntryFlagEnum.HERETIC_DESTRUCTION_1))
		  actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.HERETIC_DESTRUCTION_1.toString,UserEntryFlagEnum.HERETIC_DESTRUCTION_2.toString))
      }
	}
	
	//異端不知羞恥派對 奢靡之宴　宵禁
	val heretic_partys = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERETIC_PARTY.toString)
	heretic_partys.foreach { heretic_party =>
	  val actioner = user_entrys.filter(_.id.is == heretic_party.actioner_id.is)(0)
	  val live_pontiff = user_entrys.filter(x=>(x.current_role == RoleEnum.PONTIFF) && (x.live.is))
	  val live_cleric = user_entrys.filter(x=>(x.current_role == RoleEnum.CLERIC) && (x.live.is))
	  
	  if (actioner.live.is) {
	    actioner.subrole(SubroleEnum.SUDDENDEATH.toString)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.HERETIC_PARTY.toString)
		if (live_pontiff.length == 2) {
		  live_pontiff(0).user_flags(live_pontiff(0).user_flags.is + UserEntryFlagEnum.HERETIC_PARTY_1.toString)
		  live_pontiff(1).user_flags(live_pontiff(1).user_flags.is + UserEntryFlagEnum.HERETIC_PARTY_1.toString)
		} else if (live_pontiff.length == 1) {
		  live_pontiff(0).user_flags(live_pontiff(0).user_flags.is + UserEntryFlagEnum.HERETIC_PARTY_1.toString)
		}
		if (live_cleric.length == 2) {
		  live_cleric(0).user_flags(live_cleric(0).user_flags.is + UserEntryFlagEnum.HERETIC_PARTY_2.toString)
		  live_cleric(1).user_flags(live_cleric(1).user_flags.is + UserEntryFlagEnum.HERETIC_PARTY_2.toString)
		} else if (live_cleric.length == 1) {
		  live_cleric(0).user_flags(live_cleric(0).user_flags.is + UserEntryFlagEnum.HERETIC_PARTY_2.toString)
		}
      }
	}
	
	//異端祈雨
	val heretic_pray_rains = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERETIC_PRAY_RAIN.toString)
	heretic_pray_rains.foreach { heretic_pray_rain =>
	  val actioner = user_entrys.filter(_.id.is == heretic_pray_rain.actioner_id.is)(0)
	  
	  if (actioner.live.is) {
	    actioner.action_point(actioner.action_point.is - 6)
		room_day.weather(WeatherEnum.RAINY.toString)
      }
	}

    // 道具薩滿冠冕
    //val item_shaman_crowns = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_SHAMAN_CROWN.toString)
    //item_shaman_crowns.foreach { item_shaman_crown =>
    //  val actioner = user_entrys.filter(_.id.is == item_shaman_crown.actioner_id.is)(0)
    //  if (actioner.live.is) {
    //    actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
    //  }
    //}

    // 狂巫法術
    val sorceror_augures = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_AUGURE.toString)
    sorceror_augures.foreach { sorceror_augure =>
      val actioner = user_entrys.filter(_.id.is == sorceror_augure.actioner_id.is)(0)
      if (actioner.live.is) {
        val target   = user_entrys.filter(_.id.is == sorceror_augure.actionee_id.is)(0)
        if (target.current_role == RoleEnum.GODFAT) {
          actioner.live(false)

          val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                        .actioner_id(actioner.id.is).mtype(MTypeEnum.DEATH_GODFAT.toString)
          sys_mes.save
        } else if (target.current_role == RoleEnum.DEMON) {
          target.action_point(target.action_point.is + 2)
          //x target.save
        }
        actioner.action_point(Math.max(0, actioner.action_point.is-2))
        //x actioner.save
      }
    }
    val sorceror_whispers = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_WHISPER.toString)
    sorceror_whispers.foreach { sorceror_whisper =>
      val actioner = user_entrys.filter(_.id.is == sorceror_whisper.actioner_id.is)(0)
      if (actioner.live.is) {
        //if (room.has_flag(RoomFlagEnum.SORCEROR_WHISPER1))
        //  actioner.action_point(Math.max(0, actioner.action_point.is-2))
        //else
          actioner.action_point(Math.max(0, actioner.action_point.is-3))
        //x actioner.save
      }
    }
    val sorceror_conjures = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_CONJURE.toString)
    sorceror_conjures.foreach { sorceror_conjure =>
      val actioner = user_entrys.filter(_.id.is == sorceror_conjure.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == sorceror_conjure.actionee_id.is)(0)

      if (actioner.live.is) {
        if (target.current_role == RoleEnum.GODFAT) {
          process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_GODFAT)
          /*
          actioner.live(false)

          val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                        .actioner_id(actioner.id.is).mtype(MTypeEnum.DEATH_GODFAT.toString)
          sys_mes.save
          */
        } else if (target.current_role == RoleEnum.DEMON) {
          target.action_point(target.action_point.is + 5)
          //x target.save
        } else if (target.current_role == RoleEnum.FOX) {
          val fox_barriers = fox_barrier_votes.filter(_.actioner_id.is == target.id.is)
          if (fox_barriers.length == 0)
            process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_FOX)
          /*
          target.live(false)
          target.save

          val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                        .actioner_id(target.id.is).mtype(MTypeEnum.DEATH_SORCEROR.toString)
          sys_mes.save
          */
        } else if (target.current_role == RoleEnum.PONTIFF) {
          process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_SORCEROR)
        }

        actioner.action_point(Math.max(0, actioner.action_point.is-4))
        //x actioner.save
      }
    }
    val sorceror_shouts = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_SHOUT.toString)
    sorceror_shouts.foreach { sorceror_shout =>
      val actioner = user_entrys.filter(_.id.is == sorceror_shout.actioner_id.is)(0)
      if (actioner.live.is) {
        if (room.has_flag(RoomFlagEnum.SORCEROR_SHOUT1))
          actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.SHOUTED.toString)
        actioner.action_point(Math.max(0, actioner.action_point.is-5))
        //x actioner.save
      }
    }
    val sorceror_summons = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_SUMMON.toString)
    sorceror_summons.foreach { sorceror_summon =>
      val actioner = user_entrys.filter(_.id.is == sorceror_summon.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.action_point(Math.max(0, actioner.action_point.is-5))
        actioner.user_flags(actioner.user_flags.is.toString + UserEntryFlagEnum.SUMMON.toString)
        var live_summons = 0
        user_entrys.foreach {summon_user =>
          if (summon_user.live.is)
            live_summons += summon_user.user_flags.is.filter(_ == UserEntryFlagEnum.SUMMON.toString()(0)).length
        }
        val talk_sentence = "＜＜狂巫召喚狼元素＞＞ (目前數量：" + live_summons.toString + ")"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }
    val sorceror_believes = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_BELIEVE.toString)
    sorceror_believes.foreach { sorceror_believe =>
      val actioner = user_entrys.filter(_.id.is == sorceror_believe.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == sorceror_believe.actionee_id.is)(0)

      if (actioner.live.is)
      {
        actioner.action_point(Math.max(0, actioner.action_point.is-5))
        //x actioner.save
        if ((RoleEnum.get_role(target.role.is.substring(0,1)).role_side == RoomVictoryEnum.VILLAGER_WIN) &&
          (target.subrole.is == "")) {
          target.subrole(SubroleEnum.WOLFBELIEVER.toString)
          //x target.save
        }
      }
    }
    user_entrys.foreach { sear_entry =>
      val sear_length = sear_entry.user_flags.is.filter(_ == UserEntryFlagEnum.SEAR.toString()(0)).length
      if (sear_length >= 3)
        sear_entry.user_flags(sear_entry.user_flags.is.replace(UserEntryFlagEnum.SEAR.toString,""))
      else if (sear_length > 0)
        sear_entry.user_flags(sear_entry.user_flags.is + UserEntryFlagEnum.SEAR.toString)
    }
    val sorceror_sears = votes.filter(_.mtype.is == MTypeEnum.VOTE_SORCEROR_SEAR.toString)
    sorceror_sears.foreach { sorceror_sear =>
      val actioner = user_entrys.filter(_.id.is == sorceror_sear.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == sorceror_sear.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is))
      {
        actioner.action_point(Math.max(0, actioner.action_point.is-3))
        target.user_flags(target.user_flags.is.toString + UserEntryFlagEnum.SEAR.toString)
      }

    }

    // 冰封
    val iced1s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_1))
    iced1s.foreach { iced1 =>
      val disrupts = votes.filter(_.actionee_id.is == iced1.id.is)

      if ((iced1.live.is) && (disrupts.length == 0)) {
        process_death(room, room_day, iced1, user_entrys, MTypeEnum.DEATH_PENGUIN_ICE)
        val penguins = user_entrys.filter( x =>
          (x.current_role == RoleEnum.PENGUIN)) // && (x.live.is))
        penguins.foreach { penguin =>
          penguin.action_point(penguin.action_point.is+1)
          //x live_penguin.save
        }
      } else {
        iced1.user_flags(iced1.user_flags.is.replace(UserEntryFlagEnum.ICED_1.toString, ""))
        //x iced1.save
      }
       
    }
    val iced2s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_2))
    iced2s.foreach { iced2 =>
      val disrupts = votes.filter(_.actionee_id.is == iced2.id.is)

      if ((iced2.live.is) && (disrupts.length == 0))
        iced2.user_flags(iced2.user_flags.is.replace(UserEntryFlagEnum.ICED_2.toString,UserEntryFlagEnum.ICED_1.toString))
      else
        iced2.user_flags(iced2.user_flags.is.replace(UserEntryFlagEnum.ICED_2.toString, ""))

      //x iced2.save
    }
    val iced3s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_3))
    iced3s.foreach { iced3 =>
      val disrupts = votes.filter(_.actionee_id.is == iced3.id.is)

      if ((iced3.live.is) && (disrupts.length == 0))
        iced3.user_flags(iced3.user_flags.is.replace(UserEntryFlagEnum.ICED_3.toString,UserEntryFlagEnum.ICED_2.toString))
      else
        iced3.user_flags(iced3.user_flags.is.replace(UserEntryFlagEnum.ICED_3.toString, ""))

      //x iced3.save
    }
    /* val iced4s  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_4))
    iced4s.foreach { iced4 =>
      val disrupts = votes.filter(_.actionee_id.is == iced4.id.is)

      if ((iced4.live.is) && (disrupts.length == 0))
        iced4.user_flags(iced4.user_flags.is.replace(UserEntryFlagEnum.ICED_4.toString,UserEntryFlagEnum.ICED_3.toString))
      else
        iced4.user_flags(iced4.user_flags.is.replace(UserEntryFlagEnum.ICED_4.toString, ""))

      iced4.save
    } */
	
    val penguin_ice_votes = votes.filter(x => (x.mtype.is == MTypeEnum.VOTE_PENGUIN_ICE.toString) ||
                                              (x.mtype.is == MTypeEnum.VOTE_PENGUIN_CHILL.toString))
    penguin_ice_votes.foreach { penguin_ice_vote =>
      val actioner = user_entrys.filter(_.id.is == penguin_ice_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == penguin_ice_vote.actionee_id.is)(0)
      val disrupts = votes.filter(x => (x.actionee_id.is == target.id.is)  &&
                                  (x.mtype.is != MTypeEnum.VOTE_PENGUIN_ICE.toString) &&
                                  (x.mtype.is != MTypeEnum.VOTE_PENGUIN_CHILL.toString))

      if ((actioner.live.is) && (target.live.is) && // (disrupts.length == 0) &&
          (target.current_role != RoleEnum.DEMON)) {
        if ((room.has_flag(RoomFlagEnum.PENGUIN_OPTION1)) && (room_day.weather.is == WeatherEnum.SNOWY.toString) ||
                                                             (room_day.weather.is == WeatherEnum.MISTY.toString))
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.ICED_2.toString)
        else
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.ICED_3.toString)
        //x target.save
      }
    }

    val penguin_chill_votes =
      if (room.has_flag(RoomFlagEnum.PENGUIN_OPTION3))
        votes.filter(_.mtype.is == MTypeEnum.VOTE_PENGUIN_ICE.toString)
      else
        votes.filter(_.mtype.is == MTypeEnum.VOTE_PENGUIN_CHILL.toString)
    penguin_chill_votes.foreach { penguin_chill_vote =>
      val actioner = user_entrys.filter(_.id.is == penguin_chill_vote.actioner_id.is)(0)
      val chilled_votes = votes.filter(_.actionee_id.is == actioner.id.is)
      chilled_votes.foreach { chilled_vote =>
        val target   = user_entrys.filter(_.id.is == chilled_vote.actioner_id.is)(0)
        val disrupts = votes.filter(x => (x.actionee_id.is == target.id.is)  &&
                                    (x.mtype.is != MTypeEnum.VOTE_PENGUIN_ICE.toString) &&
                                    (x.mtype.is != MTypeEnum.VOTE_PENGUIN_CHILL.toString))

        if ((actioner.live.is) && (target.live.is) && //(disrupts.length == 0) &&
            (target.current_role != RoleEnum.DEMON)) {
          if ((room_day.weather.is == WeatherEnum.SNOWY.toString) && (room.has_flag(RoomFlagEnum.PENGUIN_OPTION1)))
            target.user_flags(target.user_flags.is + UserEntryFlagEnum.ICED_2.toString)
          else
            target.user_flags(target.user_flags.is + UserEntryFlagEnum.ICED_3.toString)
          //x target.save
        }
      }

      actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.CHILL_USED.toString)
      //x actioner.save
    }

    // 教主 STUN 回復
    val pontiffs  =  user_entrys.filter(_.current_role == RoleEnum.PONTIFF)
    pontiffs.foreach { pontiff =>
      if (pontiff.user_flags.is.indexOf( UserEntryFlagEnum.PONTIFF_STUNNED.toString ) != -1 ) {
         pontiff.user_flags(pontiff.user_flags.is.replace(UserEntryFlagEnum.PONTIFF_STUNNED.toString,""))
         //x pontiff.save
      }
    }

    val pontiff_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_PONTIFF.toString)
    // 教主拉人入教
    pontiff_votes.foreach { pontiff_vote =>
      val actioner = user_entrys.filter(_.id.is == pontiff_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == pontiff_vote.actionee_id.is)(0)
	  val other_lovers = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LOVER)) && (x.live.is) &&
										  (x.id.is != actioner.id.is) && (x.has_flag(UserEntryFlagEnum.NORELIGION)))
	  val live_lovers = user_entrys.filter(x=>(x.has_flag(UserEntryFlagEnum.LOVER)) && (x.live.is))

      if ((archmage_dispell_votes.filter(_.actionee_id.is == actioner.id.is).length == 0) &&
          (actioner.live.is)) {
		if ((actioner.has_flag(UserEntryFlagEnum.LOVER)) && (target.has_flag(UserEntryFlagEnum.LOVER)) && (live_lovers.length <= 2)){
			target.user_flags( target.user_flags.is + UserEntryFlagEnum.RELIGION.toString )
			target.user_flags(target.user_flags.is.replace(UserEntryFlagEnum.NORELIGION.toString, ""))
		} else if (target.current_role == RoleEnum.FALLEN_ANGEL) {
        } else if ((room.has_flag(RoomFlagEnum.ROLE_FALLEN_ANGEL)) && (target.current_role == RoleEnum.DEMON)) {
		    if (target.subrole.is == SubroleEnum.NORELIGION.toString) {
		      target.user_flags( target.user_flags.is + UserEntryFlagEnum.DEMON_GIVEUP.toString )
              actioner.user_flags( actioner.user_flags.is + UserEntryFlagEnum.PONTIFF_STUNNED.toString )
            } else if (target.has_flag(UserEntryFlagEnum.NORELIGION)) {
		      target.user_flags( target.user_flags.is + UserEntryFlagEnum.DEMON_GIVEUP.toString )
            } else {
		      target.user_flags( target.user_flags.is + UserEntryFlagEnum.DEMON_GIVEUP.toString )
		      target.user_flags( target.user_flags.is + UserEntryFlagEnum.RELIGION.toString )
		    }
        } else if (target.subrole.is == SubroleEnum.NORELIGION.toString) {
          actioner.user_flags( actioner.user_flags.is + UserEntryFlagEnum.PONTIFF_STUNNED.toString )
          //x actioner.save
        } else if ((target.hasnt_flag(UserEntryFlagEnum.RELIGION)) &&
                   (target.hasnt_flag(UserEntryFlagEnum.NORELIGION))) {
          target.user_flags( target.user_flags.is + UserEntryFlagEnum.RELIGION.toString )
          //x target.save
        }
      }
    }

    val pontiff_command_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_PONTIFF_COMMAND.toString)
    // 教主指定投票
    pontiff_command_votes.foreach { pontiff_command_vote =>
      val actioner = user_entrys.filter(_.id.is == pontiff_command_vote.actioner_id.is)(0)

      if ((archmage_dispell_votes.filter(_.actionee_id.is == actioner.id.is).length == 0) &&
          (actioner.live.is)) {
        actioner.user_flags( actioner.user_flags.is + UserEntryFlagEnum.PONTIFF_COMMAND_USED.toString )
        //x actioner.save
      }
    }
	
	val pontiff_aura_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_PONTIFF_AURA.toString)
    // 教主光環
    pontiff_aura_votes.foreach { pontiff_aura_vote =>
      val actioner = user_entrys.filter(_.id.is == pontiff_aura_vote.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.user_flags( actioner.user_flags.is + UserEntryFlagEnum.PONTIFF_AURA.toString )
        //x actioner.save
      }
    }
	//魅惑解除
	val lustcharms  =  user_entrys.filter(_.has_flag(UserEntryFlagEnum.FALLENANGEL_LUSTCHARM))
    lustcharms.foreach { lustcharm =>
      if (lustcharm.live.is)
        lustcharm.user_flags(lustcharm.user_flags.is.replace(UserEntryFlagEnum.FALLENANGEL_LUSTCHARM.toString, ""))

      //x lustcharm.save
    }
	val fallenangel_lustcharm_votes = votes.filter(_.mtype.is == MTypeEnum.VOTE_FALLENANGEL_LUSTCHARM.toString)
    // 莉莉絲的魅惑
    fallenangel_lustcharm_votes.foreach { fallenangel_lustcharm_vote =>
      val actioner = user_entrys.filter(_.id.is == fallenangel_lustcharm_vote.actioner_id.is)(0)
	  val target   = user_entrys.filter(_.id.is == fallenangel_lustcharm_vote.actionee_id.is)(0)
      if ((actioner.live.is) && (target.live.is)) {
        target.user_flags( target.user_flags.is + UserEntryFlagEnum.FALLENANGEL_LUSTCHARM.toString )
        //x actioner.save
      }
    }

    // 大魔導 解除法術
    archmage_dispell_votes.foreach { archmage_dispell_vote =>
      val actioner = user_entrys.filter(_.id.is == archmage_dispell_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == archmage_dispell_vote.actionee_id.is)(0)

      if (actioner.live.is) {
        if (target.current_role == RoleEnum.FOX) {
          process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_FOX)
        } else {
          if (target.current_role != RoleEnum.DEMON)
            target.action_point(0)

          val links_str =
            if (target.has_flag(UserEntryFlagEnum.LINKS))
              UserEntryFlagEnum.LINKS.toString
            else
              ""

          val autovoted_str =
            if (target.has_flag(UserEntryFlagEnum.AUTOVOTED))
              UserEntryFlagEnum.AUTOVOTED.toString
            else
              ""
          val religion_str =
            if (target.subrole.is == SubroleEnum.SUBPONTIFF.toString)
              UserEntryFlagEnum.RELIGION.toString
            else if (target.subrole.is == SubroleEnum.NORELIGION.toString)
              UserEntryFlagEnum.NORELIGION.toString
            else
              ""
          target.user_flags(links_str + autovoted_str + religion_str)
          //x target.save()
        }

        actioner.action_point(Math.max(actioner.action_point.is -3, 0))
        //x actioner.save()
      }
    }
    // 大魔導 召喚水元素
    val archmage_summon_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_ARCHMAGE_SUMMON.toString)
    archmage_summon_votes.foreach { archmage_summon_vote =>
      val actioner = user_entrys.filter(_.id.is == archmage_summon_vote.actioner_id.is)(0)
      if ((actioner.live.is) && (actioner.has_flag(UserEntryFlagEnum.WATER_ELEM_USED))) {
         actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.WATER_ELEM_USED.toString, ""))
         actioner.action_point(Math.max(actioner.action_point.is -3, 0))
         //x actioner.save()
      }
    }

    // 學者調查能力
    val scholar_examines = votes.filter(x => (x.mtype.is == MTypeEnum.VOTE_SCHOLAR_EXAMINE.toString) ||
                                             (x.mtype.is == MTypeEnum.VOTE_SCHOLAR_EXAMINE2.toString))
    scholar_examines.foreach { scholar_examine =>
      val actioner = user_entrys.filter(_.id.is == scholar_examine.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == scholar_examine.actionee_id.is)(0)

      if ((actioner.live.is) && (target.hasnt_flag(UserEntryFlagEnum.RELIGION)) &&
          (target.current_role != RoleEnum.PONTIFF)) {
        if (target.hasnt_flag(UserEntryFlagEnum.NORELIGION)) {
          target.user_flags( target.user_flags.is + UserEntryFlagEnum.NORELIGION.toString )
          //x target.save
        }

      }

      if (scholar_examine.mtype.is == MTypeEnum.VOTE_SCHOLAR_EXAMINE2.toString)
        actioner.user_flags( actioner.user_flags.is + UserEntryFlagEnum.ALTERNATE.toString )

      actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.CARD_JUSTICE.toString, ""))
      //x actioner.save
    }

    // 暴民模式
    var become_mobs = votes.filter(_.mtype.is == MTypeEnum.VOTE_BECOMEMOB.toString)
    if (room.has_flag(RoomFlagEnum.MOB_MODE1))
      become_mobs = become_mobs.take(2)
    become_mobs.foreach { become_mob =>
      val actioner = user_entrys.filter(_.id.is == become_mob.actioner_id.is)(0)

      if ((actioner.live.is) && (actioner.hasnt_flag(UserEntryFlagEnum.BECAME_MOB))) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.BECAME_MOB.toString)
        //x actioner.save
      }
    }

    // 若狐全掛，背德連帶死亡
    // 若狼全掛，幼狼連帶死亡
    process_followers(room, room_day, user_entrys)

    val madman_suicide_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_MADMAN_SUICIDE.toString)
    // 狂人自爆
    madman_suicide_votes.foreach { madman_suicide_vote =>
      val actioner = user_entrys.filter(_.id.is == madman_suicide_vote.actioner_id.is)(0)

      if (actioner.live.is) {
        process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_MADMAN)
      }
    }

    // 道具死亡筆記
    val item_death_notes = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_DEATH_NOTE.toString)
    item_death_notes.foreach { item_death_note =>
      val actioner = user_entrys.filter(_.id.is == item_death_note.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == item_death_note.actionee_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)

        if (target.live.is)
          process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_DEATH_NOTE)
	    val talk_sentence =  "＜＜昨晚有人的名字被寫在死亡筆記上了＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }
	
	val fallenangel_gluttonyhunger_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_FALLENANGEL_GLUTTONYHUNGER.toString)
    // 別西卜的飢餓
    fallenangel_gluttonyhunger_votes.foreach { fallenangel_gluttonyhunger_vote =>
      val actioner = user_entrys.filter(_.id.is == fallenangel_gluttonyhunger_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == fallenangel_gluttonyhunger_vote.actionee_id.is)(0)
      
      if ((actioner.live.is) && (target.live.is)) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.FALLENANGEL_HAVEUSED.toString)
        //x actioner.save

        process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_GLUTTONY_HUNGER)
      }
    }

    val herbalist_poison_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERBALIST_POISON.toString)
    // 藥師使用毒藥
    herbalist_poison_votes.foreach { herbalist_poison_vote =>
      val actioner = user_entrys.filter(_.id.is == herbalist_poison_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == herbalist_poison_vote.actionee_id.is)(0)
      
      if ((actioner.live.is) && (target.live.is)) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.POISON_USED.toString)
        //x actioner.save

        process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_POISON_H)
      }
    }

    val alchemist_poison_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_ALCHEMIST_POISON.toString)
    // 鍊金使用毒藥
    alchemist_poison_votes.foreach { alchemist_poison_vote =>
      val actioner = user_entrys.filter(_.id.is == alchemist_poison_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == alchemist_poison_vote.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is)) {
        actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.EARTH.toString,"").replace(UserEntryFlagEnum.FIRE.toString,""))
        //x actioner.save

        process_death(room, room_day, target, user_entrys, MTypeEnum.DEATH_POISON_H)
      }
    }
	
	val spy_over_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_SPY_OVER.toString)
    // 間諜任務完成
    spy_over_votes.foreach { spy_over_vote =>
      val actioner = user_entrys.filter(_.id.is == spy_over_vote.actioner_id.is)(0)

      if (actioner.live.is) {
        process_death(room, room_day, actioner, user_entrys, MTypeEnum.DEATH_SPY)
		actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.SPY_OVER.toString)
      }
    }
	
	// 清除干擾
	val jamns = user_entrys.filter(_.has_flag(UserEntryFlagEnum.SPY_JAM))
	if (jamns.length != 0) {
	  jamns.foreach { jamn =>
          jamn.user_flags(jamn.user_flags.is.replace(UserEntryFlagEnum.SPY_JAM.toString,""))
      }
	}
	
	val spy_jam_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_SPY_JAM.toString)
    // 間諜干擾
    spy_jam_votes.foreach { spy_jam_vote =>
      val actioner = user_entrys.filter(_.id.is == spy_jam_vote.actioner_id.is)(0)
	  val target   = user_entrys.filter(_.id.is == spy_jam_vote.actionee_id.is)(0)

      if (actioner.live.is) {
		  actioner.action_point(actioner.action_point - 1)
		if((target.current_role == RoleEnum.PONTIFF) && (room_day.day_no.is >= 12)) {
		  target.user_flags(target.user_flags.is + UserEntryFlagEnum.SPY_JAM.toString)
		} else if((target.current_role == RoleEnum.AUGURER) ||
				  (target.current_role == RoleEnum.NECROMANCER) ||
				  (target.current_role == RoleEnum.SCHOLAR) ||
				  (target.current_role == RoleEnum.BETRAYER) ||
				  (target.current_role == RoleEnum.GODFAT)){
		  target.user_flags(target.user_flags.is + UserEntryFlagEnum.SPY_JAM.toString)
		}
		
      }
    }

    val herbalist_mix_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERBALIST_MIX.toString)
    // 藥師使用製藥
    herbalist_mix_votes.foreach { herbalist_mix_vote =>
      val actioner = user_entrys.filter(_.id.is == herbalist_mix_vote.actioner_id.is)(0)

      if (actioner.live.is)  {
        val mix_result_int = random.nextInt(5)
        mix_result_int match
        {
          case 0 => actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.ELIXIR_USED.toString, "")) //x .save
          case 1 => actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.POISON_USED.toString, "")) //x .save
          case 2 => val herbalist_mix_new_vote = Vote.create.roomday_id(room_day.id.is).mtype(MTypeEnum.VOTE_BETRAYER_DISGUISE.toString).actioner_id(0).actionee_id(actioner.id.is)
                    //herbalist_mix_new_vote.save
                    votes_for_save = votes_for_save ::: List(herbalist_mix_new_vote)
          case 3 => val herbalist_mix_new_vote = Vote.create.roomday_id(room_day.id.is).mtype(MTypeEnum.VOTE_BETRAYER_FOG.toString).actioner_id(0)
                    //herbalist_mix_new_vote.save
                    votes_for_save = votes_for_save ::: List(herbalist_mix_new_vote)
          case 4 => actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.DEATH_2.toString) //x .save
        }
      }
    }

    val herbalist_drop_votes   = votes.filter(_.mtype.is == MTypeEnum.VOTE_HERBALIST_DROP.toString)
    // 藥師放棄藥品
    herbalist_drop_votes.foreach { herbalist_drop_vote =>
      val actioner = user_entrys.filter(_.id.is == herbalist_drop_vote.actioner_id.is)(0)

      if (actioner.live.is)  {
	    
        if (room_day.day_no == 1) {
          actioner.role(RoleEnum.ALCHEMIST.toString + actioner.role.is + actioner.role.is.toString.substring(1))
          val init_essence = UserEntryFlagEnum.EARTH.toString + UserEntryFlagEnum.AIR.toString  /* random.nextInt(6) match {
            case 0 => UserEntryFlagEnum.EARTH.toString + UserEntryFlagEnum.WATER.toString
            case 1 => UserEntryFlagEnum.EARTH.toString + UserEntryFlagEnum.AIR.toString
            case 2 => UserEntryFlagEnum.EARTH.toString + UserEntryFlagEnum.FIRE.toString
            case 3 => UserEntryFlagEnum.WATER.toString + UserEntryFlagEnum.AIR.toString
            case 4 => UserEntryFlagEnum.WATER.toString + UserEntryFlagEnum.FIRE.toString
            case _ => UserEntryFlagEnum.AIR.toString   + UserEntryFlagEnum.FIRE.toString
          } */
          actioner.user_flags(actioner.user_flags.is + init_essence + UserEntryFlagEnum.ALCHEMIST_ALTERNATE.toString)
        } else {
          if (actioner.hasnt_flag(UserEntryFlagEnum.ELIXIR_USED))
            actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.ELIXIR_USED.toString)
          if (actioner.hasnt_flag(UserEntryFlagEnum.POISON_USED))
            actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.POISON_USED.toString)
        }
      }
    }

    // 道具封印遺書
    val item_dmessage_seals = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_DMESSAGE_SEAL.toString)
    item_dmessage_seals.foreach { item_dmessage_seal =>
      val actioner = user_entrys.filter(_.id.is == item_dmessage_seal.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == item_dmessage_seal.actionee_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)

        if (target.hasnt_flag(UserEntryFlagEnum.DMESSAGE_SEALED))
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.DMESSAGE_SEALED.toString)
	  
	    val talk_sentence =  "＜＜昨晚有人的遺書被封印了＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    // 道具不運錢包
    val item_unlucky_purses = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_UNLUCKY_PURSE.toString)
    item_unlucky_purses.foreach { item_unlucky_purse =>
      val actioner = user_entrys.filter(_.id.is == item_unlucky_purse.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == item_unlucky_purse.actionee_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)

        if (target.live.is) {
		  if (actioner.get_fallenangel_special == RoleSpecialEnum.GREED.toString) {
            actioner.cash(actioner.cash.is + target.cash.is)
	    } else {
		    actioner.cash(actioner.cash.is + target.cash.is / 2)
		}
            target.cash(0)
        }
		val talk_sentence =  "＜＜昨晚有人的金錢被偷光了＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    //道具祝福之杖
	val item_bless_staffS = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_BLESS_STAFF.toString)
    item_bless_staffS.foreach { item_bless_staff =>
      val actioner = user_entrys.filter(_.id.is == item_bless_staff.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		
		val talk_sentence =  "＜＜昨晚祝福之杖發出了光芒＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }
	
	//道具咒縛黑羽
	val item_black_feathes = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_BLACK_FEATHER.toString)
    item_black_feathes.foreach { item_black_feathe =>
      val actioner = user_entrys.filter(_.id.is == item_black_feathe.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		
		val talk_sentence =  "＜＜昨晚咒縛黑羽帶來了詛咒＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }
	
	//道具腹語娃娃
	val item_ventriloquists = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_VENTRILOQUIST.toString)
    item_ventriloquists.foreach { item_ventriloquist =>
      val actioner = user_entrys.filter(_.id.is == item_ventriloquist.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		
		val talk_sentence =  "＜＜今天有人受到腹語娃娃的幫助＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }
	
	//道具薩滿冠冕
	val item_shaman_crowns = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_SHAMAN_CROWN.toString)
    item_shaman_crowns.foreach { item_shaman_crown =>
      val actioner = user_entrys.filter(_.id.is == item_shaman_crown.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		
		val talk_sentence =  "＜＜昨晚似乎有人使用了薩滿冠冕＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }
	
	//道具人口普查
	val item_population_censuss = item_votes.filter(_.mtype.is == MTypeEnum.ITEM_POPULATION_CENSUS.toString)
    item_population_censuss.foreach { item_population_census =>
      val actioner = user_entrys.filter(_.id.is == item_population_census.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
		
		val talk_sentence =  "＜＜昨晚似乎有人使用了人口普查＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    val madman_stun1_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_MADMAN_STUN1.toString)
    // 狂人擊昏1
    madman_stun1_votes.foreach { madman_stun_vote1 =>
      val actioner = user_entrys.filter(_.id.is == madman_stun_vote1.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == madman_stun_vote1.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is)) {
        actioner.action_point(Math.max(actioner.action_point.is -1, 0))
        //x actioner.save

        if (RoleEnum.get_role(target.current_role).role_side == RoomVictoryEnum.VILLAGER_WIN)
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.STUNNED_1.toString)
        else
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.STUNNED_2.toString)
        //x target.save
      }
    }


    val madman_stun3_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_MADMAN_STUN3.toString)
    // 狂人擊昏3
    madman_stun3_votes.foreach { madman_stun_vote3 =>
      val actioner = user_entrys.filter(_.id.is == madman_stun_vote3.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == madman_stun_vote3.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is)) {
        actioner.action_point(Math.max(actioner.action_point.is -2, 0))
        actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.CARD_STRENGTH.toString, ""))
        //x actioner.save

        target.user_flags(target.user_flags.is + UserEntryFlagEnum.STUNNED_3.toString)
        //x target.save
      }
    }
	
	val fallenangel_slothworth_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_FALLENANGEL_SLOTHWORTH.toString)
    // 貝利爾的價值
    fallenangel_slothworth_votes.foreach { fallenangel_slothworth_vote =>
      val actioner = user_entrys.filter(_.id.is == fallenangel_slothworth_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == fallenangel_slothworth_vote.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is) && (target.subrole.is == "")) {
        actioner.user_flags(actioner.user_flags.is + UserEntryFlagEnum.FALLENANGEL_HAVEUSED.toString)
        //x actioner.save

        target.user_flags(target.user_flags.is + UserEntryFlagEnum.STUNNED_3.toString)
        //x target.save
      }
    }

    val madman_stun_votes     = votes.filter(_.mtype.is == MTypeEnum.VOTE_MADMAN_STUN.toString)
    // 狂人擊忘
    madman_stun_votes.foreach { madman_stun_vote =>
      val actioner = user_entrys.filter(_.id.is == madman_stun_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == madman_stun_vote.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is) &&
          (target.subrole.is == "")) {
        actioner.action_point(Math.max(actioner.action_point.is -2, 0))
        //x actioner.save

        target.subrole(SubroleEnum.MEMORYLOSS8.toString)
        //x target.save
      }
    }

    // 模仿師醒來時變成卡片師
    if (room.has_flag(RoomFlagEnum.ROLE_CARDMASTER)) {
      user_entrys.filter(x=>((x.current_role == RoleEnum.SHIFTER) && (x.live.is))).foreach{shifter =>
      if (((shifter.subrole.is.indexOf(SubroleEnum.MEMORYLOSS4.toString) != -1 ) &&
           (room_day.day_no.is == 7)) ||
          ((shifter.subrole.is.indexOf(SubroleEnum.MEMORYLOSS6.toString) != -1 ) &&
           (room_day.day_no.is == 11)) ||
          ((shifter.subrole.is.indexOf(SubroleEnum.MEMORYLOSS8.toString) != -1 ) &&
           (room_day.day_no.is == 15)) ||
          ((shifter.subrole.is.indexOf(SubroleEnum.FAKEAUGURER.toString) != -1 ) &&
           (room_day.day_no.is == 7)))
        shifter.role(RoleEnum.CARDMASTER.toString)
      }
		
      if (room.has_flag(RoomFlagEnum.CARDMASTER_OPTION1)) {
          user_entrys.filter(x=>((x.current_role == RoleEnum.CARDMASTER) && (x.live.is))).foreach{cardmaster =>
            var card_pool : java.util.LinkedList[String] = new java.util.LinkedList()
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_FOOL)) card_pool.add(UserEntryFlagEnum.CARD_FOOL.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_MAGICIAN)) card_pool.add(UserEntryFlagEnum.CARD_MAGICIAN.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_CHARIOT)) card_pool.add(UserEntryFlagEnum.CARD_CHARIOT.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_HERMIT)) card_pool.add(UserEntryFlagEnum.CARD_HERMIT.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_STRENGTH)) card_pool.add(UserEntryFlagEnum.CARD_STRENGTH.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_JUSTICE)) card_pool.add(UserEntryFlagEnum.CARD_JUSTICE.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_TOWER)) card_pool.add(UserEntryFlagEnum.CARD_TOWER.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_SUN)) card_pool.add(UserEntryFlagEnum.CARD_SUN.toString)
            if (card_pool.size() != 0) {
              java.util.Collections.shuffle(card_pool)
              cardmaster.user_flags(cardmaster.user_flags.is + card_pool.removeFirst())
              //x cardmaster.save
            }
          }
	    } else if (room_day.day_no.is >= 7) {
          user_entrys.filter(x=>((x.current_role == RoleEnum.CARDMASTER) && (x.live.is))).foreach{cardmaster =>
            var card_pool : java.util.LinkedList[String] = new java.util.LinkedList()
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_FOOL)) card_pool.add(UserEntryFlagEnum.CARD_FOOL.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_MAGICIAN)) card_pool.add(UserEntryFlagEnum.CARD_MAGICIAN.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_CHARIOT)) card_pool.add(UserEntryFlagEnum.CARD_CHARIOT.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_HERMIT)) card_pool.add(UserEntryFlagEnum.CARD_HERMIT.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_STRENGTH)) card_pool.add(UserEntryFlagEnum.CARD_STRENGTH.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_JUSTICE)) card_pool.add(UserEntryFlagEnum.CARD_JUSTICE.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_TOWER)) card_pool.add(UserEntryFlagEnum.CARD_TOWER.toString)
            if (cardmaster.hasnt_flag(UserEntryFlagEnum.CARD_SUN)) card_pool.add(UserEntryFlagEnum.CARD_SUN.toString)
            if (card_pool.size() != 0) {
              java.util.Collections.shuffle(card_pool)
              cardmaster.user_flags(cardmaster.user_flags.is + card_pool.removeFirst())
              //x cardmaster.save
            }
          }
	    }
    }

    // 墮天使墮落
    val fallen_votes  = votes.filter(_.mtype.is == MTypeEnum.VOTE_FALLENANGEL_FALLEN.toString)
    fallen_votes.foreach { fallen_vote =>
      val actioner = user_entrys.filter(_.id.is == fallen_vote.actioner_id.is)(0)
      val target   = user_entrys.filter(_.id.is == fallen_vote.actionee_id.is)(0)

      if ((actioner.live.is) && (target.live.is))  {
        if (target.current_role == RoleEnum.PONTIFF)
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.FALLEN.toString + UserEntryFlagEnum.FALLEN.toString)
        else if (target.current_role == RoleEnum.DEMON)
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.FALLEN.toString + UserEntryFlagEnum.FALLEN.toString)
		else
          target.user_flags(target.user_flags.is + UserEntryFlagEnum.FALLEN.toString)
      }

    }

    // 背德粉紅迷霧
    val betrayer_fogs = votes.filter(_.mtype.is == MTypeEnum.VOTE_BETRAYER_FOG.toString)
    betrayer_fogs.foreach { betrayer_fog =>
      val actioner = user_entrys.filter(_.id.is == betrayer_fog.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.action_point(Math.max(actioner.action_point.is - 4, 0))
        //x actioner.save
      }
    }

    // 大魔導回復法力
    user_entrys.filter(x=>((x.current_role == RoleEnum.ARCHMAGE) && (x.live.is))).foreach{archmage =>
      archmage.action_point(Math.min(archmage.action_point.is + 1, 10))
      //x archmage.save
    }

    // 若狐全掛，背德連帶死亡
    // 若狼全掛，幼狼連帶死亡
    process_followers(room, room_day, user_entrys)

    // 如果場上只剩一位村民，則轉職成大魔導
    if ((room.has_flag(RoomFlagEnum.ROLE_ARCHMAGE)) &&
        (user_entrys.length >= 25) &&
        (room_day.day_no.is == 15)) {
       val live_villagers = user_entrys.filter( x => (x.live.is) && (x.current_role == RoleEnum.VILLAGER))
       val live_archmages = user_entrys.filter( x => (x.live.is) && (x.current_role == RoleEnum.ARCHMAGE))

       if ((live_villagers.length == 1) && (live_archmages.length == 0)){
         val live_villager = live_villagers(0)
         live_villager.role(RoleEnum.ARCHMAGE.toString + live_villager.role.is.substring(1))
         live_villager.action_point(2)
         //x live_villager.save
       }
    }

    // 道具天候棒
    val item_weather_rods = item_votes.filter{ x => (x.mtype.is == MTypeEnum.ITEM_WEATHER_ROD.toString )}
    item_weather_rods.foreach { item_weather_rod =>
      val actioner = user_entrys.filter(_.id.is == item_weather_rod.actioner_id.is)(0)
      if (actioner.live.is) {
        actioner.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
        room_day.weather(item_weather_rod.vote_flags.is)
		
		val talk_sentence =  "＜＜今天的天氣忽然被改變了＞＞"
        val talk = Talk.create.mtype(MTypeEnum.MESSAGE_GENERAL.toString).message(talk_sentence).font_type("12")
        talks_for_save = talks_for_save ::: List(talk)
      }
    }

    // 邱比特單日
    if (room.has_flag(RoomFlagEnum.CUBIC_IMMEDIATE)) {
      val cubic_holders = user_entrys.filter(_.item_flags.is == ItemEnum.CUBIC_ARROW.toString)
      cubic_holders.foreach { cubic_holder =>
        cubic_holder.item_flags(ItemEnum.ITEM_NO_ITEM.toString)
      }
    }

    val card_sun_votes  = votes.filter(_.mtype.is == MTypeEnum.VOTE_CARD_SUN.toString)
    card_sun_votes.foreach { vote =>
      val actioner = user_entrys.filter(_.id.is == vote.actioner_id.is)(0)
      actioner.user_flags(actioner.user_flags.is.replace(UserEntryFlagEnum.CARD_SUN.toString, ""))
      //x actioner.save
      room_day.weather(WeatherEnum.SUNNY.toString)
    }

    //println("users_for_save length : " + users_for_save.length)
    //users_for_save.removeDuplicates.foreach( _.save )

    // 新的一日
    val new_room_day  = RoomDay.create.room_id(room.id.is).day_no(room_day.day_no.is + 1)
                        .vote_time(1).weather(room_day.weather.is).item(room_day.item.is)
    new_room_day.save                    
    
    (votes ::: votes_for_save).foreach { vote =>
      val sys_mes = SystemMessage.create.roomday_id(new_room_day.id.is)
                    .actioner_id(vote.actioner_id.is).actionee_id(vote.actionee_id.is)
                    .mtype(vote.mtype.is).message(vote.vote_flags.is)
      sys_mes.save
    }
    (item_votes ::: itemvotes_for_save).foreach { item_vote =>
      val sys_mes = SystemMessage.create.roomday_id(new_room_day.id.is)
                    .actioner_id(item_vote.actioner_id.is).actionee_id(item_vote.actionee_id.is)
                    .mtype(item_vote.mtype.is).message(item_vote.vote_flags.is)
      sys_mes.save
    }
	
	// 清除墮落
	val fallenangels = user_entrys.filter(x =>(x.current_role == RoleEnum.FALLEN_ANGEL))
    val live_fallenangels = user_entrys.filter(x =>(x.live.is) && (x.current_role == RoleEnum.FALLEN_ANGEL))
	val fallens = user_entrys.filter(_.has_flag(UserEntryFlagEnum.FALLEN))
	if ((fallenangels.length >= 1) && (live_fallenangels.length == 0) && (fallens.length != 0)) {
	  fallens.foreach { fallen =>
          fallen.user_flags(fallen.user_flags.is.replace(UserEntryFlagEnum.FALLEN.toString,""))
	  val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + fallen.handle_name +" 的周圍的黑暗消失了…＞＞")
      talk.save
      }
	}
	
	// 清除冰凍
	if (room.has_flag(RoomFlagEnum.PENGUIN_OPTION4)) {
	  val penguins = user_entrys.filter(x =>(x.current_role == RoleEnum.PENGUIN))
      val live_penguins = user_entrys.filter(x =>(x.live.is) && (x.current_role == RoleEnum.PENGUIN))
	  val iced1_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_1))
	  val iced2_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_2))
	  val iced3_users = user_entrys.filter(_.has_flag(UserEntryFlagEnum.ICED_3))
	  if ( (penguins.length >= 1) && (live_penguins.length == 0)) {
	    if (iced1_users.length != 0){
	      iced1_users.foreach { iced1_user =>
          iced1_user.user_flags(iced1_user.user_flags.is.replace(UserEntryFlagEnum.ICED_1.toString,""))
          val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + iced1_user.handle_name +" 身邊的冰晶融化了＞＞")
          talk.save
        }
	   }
	  if (iced2_users.length != 0){
	    iced2_users.foreach { iced2_user =>
          iced2_user.user_flags(iced2_user.user_flags.is.replace(UserEntryFlagEnum.ICED_2.toString,""))
          val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + iced2_user.handle_name +" 不再散發著寒氣了＞＞")
          talk.save
        }
	   }
	  if (iced3_users.length != 0){
	    iced3_users.foreach { iced3_user =>
          iced3_user.user_flags(iced3_user.user_flags.is.replace(UserEntryFlagEnum.ICED_3.toString,""))
          val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("＜＜ " + iced3_user.handle_name +" 覺得溫暖些了＞＞")
          talk.save
        }
	   }
	  }
	}
	
	// 補充異端鮮血
	  val live_heretics = user_entrys.filter(x=>(x.current_role == RoleEnum.HERETIC) && (x.live.is))
      val dead_user_nohps = user_entrys.filter(x =>(!x.live.is) && (x.hasnt_flag(UserEntryFlagEnum.HERETIC_HP)))
	  live_heretics.foreach { live_heretic =>
		if ( (live_heretics.length >= 1) && (dead_user_nohps.length >= 1)) {
		    live_heretic.action_point(live_heretic.action_point.is + dead_user_nohps.length)
        }
	  }
	  dead_user_nohps.foreach { dead_user_nohp =>
		if ( (live_heretics.length >= 1) && (dead_user_nohps.length >= 1)) {
		    dead_user_nohp.user_flags(dead_user_nohp.user_flags.is.toString + UserEntryFlagEnum.HERETIC_HP.toString)
        }
	  }

    // 儲存使用者
    user_entrys.foreach { user => user.save }
	
    // 加入模仿者訊息
    talks_for_save.foreach { talk_for_save =>
      talk_for_save.roomday_id(new_room_day.id.is)
      talk_for_save.save
    }

    // 進入下一天
    val weather_string =
      if (room.has_flag(RoomFlagEnum.WEATHER))
        " (" + WeatherEnum.get_weather(room_day.weather.is) + ")"
      else
        ""
    val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("< < 早晨來臨 第 " + ((new_room_day.day_no.is+2)/2).toString +" 日的早上開始 > >" + weather_string)
    talk.save

    if ((new_room_day.day_no.is == 6) && (room.has_flag(RoomFlagEnum.DUMMY_REVEAL)) && (!(room.has_flag(RoomFlagEnum.NO_DUMMY)))) {
      val dummy_boy = user_entrys.filter(_.uname.is == "dummy_boy")(0)
      val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_EVIL.toString)
                   .message("< < 非人側的你察覺 " + dummy_boy.handle_name.is + " 的職業是 " +
                            RoleEnum.get_role(dummy_boy.current_role).toString.length.toString + " 個字的 > >")
      talk.save
    }

    if ((new_room_day.day_no.is == 12) && (room.has_flag(RoomFlagEnum.DUMMY_REVEAL)) && (!(room.has_flag(RoomFlagEnum.NO_DUMMY)))) {
      val dummy_boy = user_entrys.filter(_.uname.is == "dummy_boy")(0)
      val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("< < 你發現 " + dummy_boy.handle_name.is + " 的職業是 " +
                            RoleEnum.get_role(dummy_boy.current_role).toString + " > >")
      talk.save
    }
	
	if ((new_room_day.day_no.is == 12) && (room.has_flag(RoomFlagEnum.NO_DUMMY)) && (!(room.has_flag(RoomFlagEnum.DUMMY_REVEAL)))) {
      val dummy_trip = user_entrys.filter(_.trip.is == "GQ5ZTY5Z")
	  if (dummy_trip.length != 0){
      val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("< < 你發現 " + dummy_trip(0).handle_name.is + " 的職業是 " +
                            RoleEnum.get_role(dummy_trip(0).current_role).toString + " > >")
      talk.save
	  }
    }

    if (room.has_flag(RoomFlagEnum.ITEM_MODE)) {
      val talk = Talk.create.roomday_id(new_room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                   .message("< < 本日競標道具：" + ItemEnum.get_item(room_day.item.is).tag_string + " > >")
      talk.save
    }
    //room.addToRoom_days(new_day)
    //room.save(flush:true)
  }
  
  // 判斷勝利條件
  def check_victory(room : Room, user_entrys: List[UserEntry]) : RoomVictoryEnum.Value = {
    var result : RoomVictoryEnum.Value = RoomVictoryEnum.NONE
    
    val live_user_entrys = user_entrys.filter(x => (x.live.is) && (x.hasnt_flag(UserEntryFlagEnum.HIDED)))

    val penguin_counts = 1 + 2 * user_entrys.filter(x => (x.current_role == RoleEnum.PENGUIN) &&
      (x.role.is.toString.indexOf(RoleEnum.INHERITER.toString) == -1)).length
    val live_penguins = user_entrys.filter( x => x.hasnt_flag(UserEntryFlagEnum.HIDED) &&
        (x.current_role == RoleEnum.PENGUIN))
    live_penguins.foreach { live_penguin =>
      if (live_penguin.action_point.is >= penguin_counts)
        return RoomVictoryEnum.PENGUIN_WIN
    }
    
    val live_human = live_user_entrys.filter( x =>
        (x.current_role != RoleEnum.WEREWOLF) &&
        (x.current_role != RoleEnum.WOLFCUB) &&
        (x.current_role != RoleEnum.FOX) &&
        (x.current_role != RoleEnum.DEMON) &&
        (x.current_role != RoleEnum.FALLEN_ANGEL) &&
        (x.current_role != RoleEnum.PENGUIN) &&
        (x.subrole.is != SubroleEnum.WOLFSTAMP.toString) &&
        ((x.current_role != RoleEnum.INHERITER) || (!room.has_flag(RoomFlagEnum.INHERITER_NEUTRAL))))
    val live_wolf     = live_user_entrys.filter(x=>(x.current_role == RoleEnum.WEREWOLF) || (x.current_role == RoleEnum.WOLFCUB))
    val live_fox      = live_user_entrys.filter(_.current_role == RoleEnum.FOX)
	val live_archmage  = live_user_entrys.filter(_.current_role == RoleEnum.ARCHMAGE)
    val live_pontiff  = live_user_entrys.filter(_.current_role == RoleEnum.PONTIFF)
    val live_religion = live_user_entrys.filter( x => (x.has_flag(UserEntryFlagEnum.RELIGION)) ||
                                                      (x.current_role == RoleEnum.PONTIFF) ||
                                                      (x.subrole.is == SubroleEnum.SUBPONTIFF.toString))
	val live_archmage_summon = live_user_entrys.filter( x => (x.hasnt_flag(UserEntryFlagEnum.WATER_ELEM_USED)) &&
                                                      (x.current_role == RoleEnum.ARCHMAGE))
    var live_summons = 0
    live_user_entrys.foreach {live_user =>
      live_summons += live_user.user_flags.is.filter(_ == UserEntryFlagEnum.SUMMON.toString()(0)).length
    }
	
	var live_archmage_summons = 0
	if ((live_archmage.length != 0) && (live_archmage_summon.length == live_archmage.length)){
	  live_archmage_summons += live_archmage_summon.length
	}

    if ((live_pontiff.length != 0) && (live_religion.length == live_user_entrys.length))
      return RoomVictoryEnum.PONTIFF_WIN

    if (live_wolf.length == 0) {
      if (live_fox.length != 0) 
        result = RoomVictoryEnum.FOX_WIN
      else if (live_human.length != 0) {
        val live_mob  = live_user_entrys.filter(x => (x.has_flag(UserEntryFlagEnum.BECAME_MOB)))

        if (live_mob.length == 1)
          result = RoomVictoryEnum.MOB_WIN
        else if  (live_mob.length == live_user_entrys.length)
          result = RoomVictoryEnum.MOB_WIN2
        else
          result = RoomVictoryEnum.VILLAGER_WIN
      }
      else
        result = RoomVictoryEnum.ABANDONED
    } else if (live_wolf.length + live_summons >= live_human.length + live_archmage_summons) {
      if (live_fox.length != 0) 
        result = RoomVictoryEnum.FOX_WIN2
      else
        result = RoomVictoryEnum.WEREWOLF_WIN
    } else {
      result = RoomVictoryEnum.NONE
      val godfat_predicter = live_user_entrys.filter(x => (x.current_role == RoleEnum.GODFAT) &&
                                                          (x.action_point.is >=4) &&
                                                          (x.has_flag(UserEntryFlagEnum.GODFAT_SPECIAL4)))
      if (godfat_predicter.length  > 0)
        result = RoomVictoryEnum.FOX_WIN2
    }

/*
    if (result != RoomVictoryEnum.NONE) {
      val live_lover = live_user_entrys.filter(_.has_flag(UserEntryFlagEnum.LOVER))
      if (live_lover.length > 1)
        result = RoomVictoryEnum.LOVER_WIN
    }
    */
    return result
  }

  def process_phase(room:Room, room_day:RoomDay, user_entrys:List[UserEntry], vote_list:List[Vote]) = {
    if (room_day.day_no.is % 2 == 0) {
      // 白天的話，要判斷是否要重新投票
      val voted_player = VoteHelper.check_vote_hang(room, room_day, user_entrys, vote_list)

      if (voted_player == null) {
        // 平手重投
        val talk = Talk.create.roomday_id(room_day.id.is).mtype(MTypeEnum.MESSAGE_GENERAL.toString)
                              .message("請重新投票(第 "+ room_day.vote_time.is.toString +" 回)")
        talk.save()

        var time_now = new java.util.Date()
        if (room_day.deadline.is != null) {
          val e_datetime = new java.util.GregorianCalendar()
          e_datetime.setTime(room_day.deadline.is)
          e_datetime.add(java.util.Calendar.MINUTE, 3)
          val e_time = e_datetime.getTime()
          if (time_now.after(e_time))
            time_now = e_time
        }

        room_day.deadline(time_now)
        room_day.vote_time(room_day.vote_time.is + 1)
        room_day.save()

        // 處理自投者
        val votes_auto = vote_list.filter(_.vote_flags.is.indexOf(VoteFlagEnum.AUTO.toString) != -1)
        votes_auto.foreach { vote_auto =>
          val auto_player = user_entrys.filter(_.id.is == vote_auto.actioner_id.is)(0)
          if (auto_player.hasnt_flag(UserEntryFlagEnum.AUTOVOTED))
            auto_player.user_flags(auto_player.user_flags.is + UserEntryFlagEnum.AUTOVOTED.toString)
          else if (auto_player.live.is) {
            auto_player.live(false)

            val talk = Talk.create.roomday_id(room_day.id.is)
                         .actioner_id(auto_player.id.is).mtype(MTypeEnum.MESSAGE_DEATHSUDDEN.toString)
            talk.save

            val sys_mes = SystemMessage.create.roomday_id(room_day.id.is)
                         .actioner_id(auto_player.id.is).mtype(MTypeEnum.DEATH_SUDDEN.toString)
            sys_mes.save

            // 若狐全掛，背德連帶死亡
            // 若狼全掛，幼狼連帶死亡
            process_followers(room, room_day, user_entrys)

          }
          //auto_player.save
        }

        user_entrys.foreach(_.save)

        var victory_check = GameProcesser.check_victory(room, user_entrys)
        if (victory_check != RoomVictoryEnum.NONE) {
          room.status(RoomStatusEnum.ENDED.toString)
          room.victory(victory_check.toString)
          room.save
        } else if (room_day.vote_time.is > 5) {
          // 直接結束，和局
          val new_day  = RoomDay.create.room_id(room.id.is).day_no(room_day.day_no.is + 1)
                                .vote_time(1)
          new_day.save()
          room.status(RoomStatusEnum.ENDED.toString)
          room.victory(RoomVictoryEnum.DRAW.toString)
          room.save()
        } else {
          // Update 使用者狀態
          DB.use(DefaultConnectionIdentifier) { conn =>
            DB.prepareStatement("update UserEntry set last_day_no = '0' where room_id = ?", conn) { stmt =>
              stmt.setLong(1, room.id.is)
              stmt.executeUpdate()
            }
          }
        }
      } else {
        // 有決定誰被吊了
        var victory_check = GameProcesser.process_day(room, room_day, user_entrys, vote_list, voted_player)
        if (victory_check == RoomVictoryEnum.NONE)
          victory_check = GameProcesser.check_victory(room, user_entrys)
        if (victory_check != RoomVictoryEnum.NONE) {
          room.status(RoomStatusEnum.ENDED.toString)
          room.victory(victory_check.toString)
          room.save
        }
      }
    } else {
      // 晚上的話就直接進行了
      GameProcesser.process_night(room, room_day, user_entrys, vote_list)
      var victory_check = GameProcesser.check_victory(room, user_entrys)
      if (victory_check != RoomVictoryEnum.NONE) {
        room.status(RoomStatusEnum.ENDED.toString)
        room.victory(victory_check.toString)
        room.save
      }
    }
  }

  // 傳回值表示是否切入下一天
  def check_deadline(room:Room, room_day:RoomDay, user_entrys:List[UserEntry]) : Boolean= {
    var result = false
    val e_datetime = new java.util.GregorianCalendar()

    // 第 0 日判斷是否廢村
    if (room_day.day_no.is == 0) {
      e_datetime.setTime(room.updated.is)
      e_datetime.add(java.util.Calendar.MINUTE, 30)
      val time_now = new java.util.Date()

      if (time_now.after(e_datetime.getTime())) {
         // 新的一日
         val new_day  = RoomDay.create.room_id(room.id.is).day_no(room_day.day_no.is + 1).vote_time(1)
         new_day.save()

         // 進入下一天
         room.status(RoomStatusEnum.ENDED.toString)
         room.victory(RoomVictoryEnum.ABANDONED.toString)
         room.save()

         return true
      }
    }

    if ((room_day.day_no.is != 0) && (room.status.is != RoomStatusEnum.ENDED.toString)) {
      e_datetime.setTime(room_day.created.is)

      if (room_day.day_no.is % 2 == 0) {
        e_datetime.add(java.util.Calendar.MINUTE, room.day_minutes.is)
      } else {
        e_datetime.add(java.util.Calendar.MINUTE, room.night_minutes.is)
      }

      val time_now = new java.util.Date()
      if ((room_day.deadline.is == null) && (time_now.after(e_datetime.getTime()))) {

        GameProcessLock.get_lock(room.id.is).synchronized {
        // 寫入最後兩分鐘還不投票將會暴斃
          val room_day2 = RoomDay.findAll(By(RoomDay.room_id, room.id.is), OrderBy(RoomDay.day_no, Descending))(0)

          if ((room_day2.deadline.is == null) && (room_day2.day_no.is == room_day.day_no.is) &&
              (room_day2.vote_time.is == room_day.vote_time.is)) {
              val talk = Talk.create.roomday_id(room_day.id.is).mtype(MTypeEnum.MESSAGE_LAST2MIN.toString)
            talk.save()

            room_day.deadline(e_datetime.getTime())
            room_day.save()

          }
        }
      }

      if (room_day.deadline.is != null) {
        val old_deadline  = room_day.deadline.is
        val real_deadline = new java.util.GregorianCalendar()
        real_deadline.setTime(room_day.deadline.is)
        real_deadline.add(java.util.Calendar.MINUTE, 2)

        val deadline = real_deadline.getTime()
        if (time_now.after(deadline)) {
          // 有人暴斃了，投票重投 @@
          GameProcessLock.get_lock(room.id.is).synchronized {
            val room_day2 = RoomDay.findAll(By(RoomDay.room_id, room.id.is), OrderBy(RoomDay.day_no, Descending))(0)

            if ((room_day2.deadline != null) && (room_day2.day_no.is == room_day.day_no.is) &&
                (room_day2.vote_time.is == room_day.vote_time.is) &&
                (room_day2.deadline.is == room_day.deadline.is)) {

              if (room_day2.deadline.is != old_deadline)
                S.error("Roomday : " + room_day.id.is.toString + " " + room_day2.deadline.is.toString + " " +
                         old_deadline.toString)

              var vote_list = Vote.findAll(By(Vote.roomday_id, room_day.id.is), By(Vote.vote_time, room_day.vote_time.is))
              if ((room_day.day_no.is %2 == 0) && (room.has_flag(RoomFlagEnum.AUTO_VOTE))) {
                // 白天的話如果有開自投的話
                user_entrys.foreach { user =>
                  if (user.get_action_list(room, room_day, user_entrys, vote_list).length != 0) {
                     val vote = Vote.create.roomday_id(room_day.id.is).actioner_id(user.id.is).vote_time(room_day.vote_time.is)
                                .actionee_id(user.id.is).mtype(MTypeEnum.VOTE_HANG.toString)
                     vote.save()
                  }
                }
                vote_list = Vote.findAll(By(Vote.roomday_id, room_day.id.is), By(Vote.vote_time, room_day.vote_time.is))
                GameProcesser.process_phase(room, room_day, user_entrys, vote_list)
                result = true
              } else {
                // 晚上或是沒開自投
                user_entrys.filter{_.get_action_list(room, room_day, user_entrys, vote_list).length != 0}.foreach { user =>
                  if (user.live.is) {
                    // 有人要暴斃了
                    GameProcesser.process_death(room, room_day, user, user_entrys, MTypeEnum.DEATH_SUDDEN)
                    //user.live(false)
                    //user.save()

                    //val sys_mes = SystemMessage.create.roomday_id(room_day.id.is).actioner_id(user.id.is)
                    //               .mtype(MTypeEnum.DEATH_SUDDEN.toString)
                    //sys_mes.save

                    val talk = Talk.create.roomday_id(room_day.id.is).actioner_id(user.id.is)
                                    .mtype(MTypeEnum.MESSAGE_DEATHSUDDEN.toString)
                    talk.save

                    // 若狐全掛，背德連帶死亡
                    // 若狼全掛，幼狼連帶死亡
                    process_followers(room, room_day, user_entrys)
                  }
                }

                user_entrys.foreach(_.save)

                // 判斷是否遊戲結束
                val victory = check_victory(room, user_entrys)
                if (victory != RoomVictoryEnum.NONE) {
                  // 新的一日
                  val new_day  = RoomDay.create.room_id(room.id.is).day_no(room_day.day_no.is + 1).vote_time(1)
                  new_day.save()

                  // 進入下一天
                  room.status(RoomStatusEnum.ENDED.toString)
                  room.victory(victory.toString)
                  room.save()

                  result = true
                } else {
                  // 投票重新開始
                  val talk2 = Talk.create.roomday_id(room_day.id.is)
                                   .mtype(MTypeEnum.MESSAGE_REVOTE.toString)
                  talk2.save

                  Vote.bulkDelete_!!(By(Vote.roomday_id, room_day.id.is), By(Vote.vote_time, room_day.vote_time.is))
                  room_day.deadline(deadline)
                  room_day.save

                  // 最後二分還不投會暴斃
                  val talk3 = Talk.create.roomday_id(room_day.id.is)
                                   .mtype(MTypeEnum.MESSAGE_LAST2MIN.toString)
                  talk3.save

                  // Update 使用者狀態
                  DB.use(DefaultConnectionIdentifier) { conn =>
                    DB.prepareStatement("update UserEntry set last_day_no = '0' where room_id = ?", conn) { stmt =>
                      stmt.setLong(1, room.id.is)
                      stmt.executeUpdate()
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return result
  }
}