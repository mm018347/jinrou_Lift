<?php
// This file will be the rewritten version of game_view.html
?>
<html>
<head>
    <?php // include('templates/head.php'); ?>
    <style type="text/css">
@import url("//fonts.googleapis.com/earlyaccess/notosanstc.css");
@import url("//fonts.googleapis.com/earlyaccess/notosansjp.css");
@import url("//fonts.googleapis.com/earlyaccess/notosanssc.css");
@import url("//fonts.googleapis.com/earlyaccess/notosanskr.css");
@import url("./css/game.css");

input, button, select, textarea, body{
  font-family: 'Noto Sans TC', 'Noto Sans JP', 'Noto Sans SC', 'Noto Sans KR', sans-serif;
}
  </style>
</head>
<body>
    <a name="#game_top"/>
    <table>
     <tr>
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
       <tr>
        <td>
          <?php // echo $game_title; ?>
        </td>
        <td align="right">
          <?php // echo $game_option; ?>
          <?php // echo $sound_object; ?>
        </td>
       </tr>
      </table>
      <?php // echo $game_info; ?>
      <table border="0" cellpadding="0" cellspacing="0">
       <tr>
        <td width="1000">
         <table border="0" cellspacing="0" cellpadding="0">
          <?php // include('templates/login_form.php'); ?>
          <form id="realtime_form" name="realtime_form">
          <tr>
           <td >第 <?php echo $day_no; ?> 日　天氣:<?php echo $weather; ?>　(生存者<?php echo $live_player; ?>人) <?php echo $auction; ?>
           <input class="left_real_time" type="text" id="realtime_output" name="realtime_output" size="50" readonly="readonly"/></td>
          </tr>
          <tr><td><span style="background-color:#CC3300;color:snow;"><?php echo $alert_message; ?></span></td></tr>
          </form>
         </table>
        </td>
        <!-- <td width=150 align=right></td> -->
       </tr>
      </table>
     </tr>

     <tr>
       <td><?php // echo $lift_msgs; ?></td>
     </tr>

     <tr>
      <table width="770" border="0" cellpadding="0" cellspacing="0">
       <tr>
        <td>
          <?php // include('templates/user_table.php'); ?>
        </td>
       </tr>
      </table>
     </tr>
     <tr>
       <?php // include('templates/role_intro.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/item_intro.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/vote_tag_d.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/messages.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/last_words_tag.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/dead_tag.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/vote_tag_n.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/self_last_words.php'); ?>
     </tr>
     <tr>
       <?php // include('templates/user_table_down.php'); ?>
     </tr>
     <tr>
     </tr>
    </table>
</body>
</html>
