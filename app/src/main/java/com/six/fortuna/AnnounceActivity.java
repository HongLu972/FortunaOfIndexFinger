package com.six.fortuna;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class AnnounceActivity extends AppCompatActivity {
    private int currentIndex = 0;
    public void addAnnounce(String e, ArrayList<String> announce){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            announce.addFirst(e);
        }else{
            announce.add(0, e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.announce);

        ArrayList<String> announces = new ArrayList<>();
        addAnnounce("修复已知部分bug\n1. 使用？！区区？！时重复引用useCard导致产生无限递归的bug\n" +
                "2. 修复作弊码中ququ无法指向艾兰市区的问题\n" +
                "3. 没找到其他bug，其实是上线前我就修了，比如艾兰虫(敌人)慈悲会打自己之类的\n" +
                "全新机制\n1. 玩过昨天的版本的人都知道状态栏里多了一项充能球，对，现在充能球已经制作完毕了！可以使用了！\n" +
                "不全新卡牌\n1. 电流相生，代码为CurrentGeneration，效果不多赘述\n" +
                "2. 雷动，代码为Thunder，生成2闪电充能球\n相信未来会有更多bug等待修", announces);
        addAnnounce("修复已知bug\n" +
                "1. 由于没绑manifest导致上个版本实际上无法看到公告，怒修之\n" +
                "2. 似乎没了\n" +
                "全新机制：冰霜充能球\n" +
                "我希望各位没忘记冰霜充能球什么效果，但我还是说一下冰霜充能球的效果吧\n" +
                "[摧毁时]获得大量护盾，使敌方迅捷-3\n" +
                "[回合结束]获得一定护盾\n" +
                "不全新卡牌\n" +
                "这次并没有全新的卡牌，难道不感觉敌人数量太少了么？所以本次更新着重于敌方目标，也就是你还玩不到冰霜充能球🤡\n" +
                "全新敌人/强度调整\n" +
                "1. [普通敌人]雷元素精灵：具有闪电充能球和充能两大强力机制的敌人，但因为其只是普通敌人，故而其数值限制了其发展\n" +
                "2. [首领]瓦伦希娜：瓦伦希娜听说自己被当作路边一条踢死了，气炸了，所以现在其获得了数项加强\n" +
                "I   瓦伦希娜不会在首回合不行动\n" +
                "II  瓦伦希娜将会闪避\n" +
                "III 瓦伦希娜解鹿在加速弹低于3的情况下不会减少本回合力量\n" +
                "IV  瓦伦希娜处置中dot提高，伤害提高，增幅提高\n" +
                "V   预知眼层数上限提高至20，使用处置后会立刻将预知眼层数设为10\n" +
                "3. [首领]里恩：赫尔墨斯让他干的\n" +
                "I  现在玩家陷入恐慌会直接导致其陷入混乱\n" +
                "II 在与里恩的战斗中，玩家的沉沦强度不会低于5\n" +
                "4. [首领]无我  秀，          \n" +
                "I  无我    自身  命  上限  害降低\n" +
                "II 无  增    度  低\n" +
                "     无  良秀  乱条  为 -0 2,  ,  . ]\n" +
                "IV       秀  得2  锁血，  发时    值上限    命  设为局外    值  0.2倍\n" +
                "V  无      不会在    护盾的  况下    败", announces);
        addAnnounce("修复已知bug/调整:\n" +
                "1.  由于之前太着急下班，导致公告区会存在异常的错误，现在修了\n" +
                "2.  忘了里恩小技能不施加沉沦，所以现在他小技能施加了\n" +
                "3.  修复无我良秀锁血后恢复的血量不受制约影响的bug\n" +
                "4.  无我良秀伤害似乎有点过高，直接翻转成负数了，故而做出了限制\n" +
                "5.  冰霜充能球现在有获得方式了！冷静头脑可以获得集中和冰霜充能球[代码：CalmBrain]\n" +
                "6.  新充能球：离子充能球，激发(摧毁)和回合开始时可以获得额外光芒(这还是月计二创么？给我创哪来了)相关卡牌：\n" +
                "   I  虹彩[代码：rainbow](3)：消耗，依次获得1个闪电，冰霜和离子充能球\n" +
                "   II 充电[代码：charge](1)：根据充能强度获得效果：<=3:获得大量充能层数并全部消耗；4-5:获得2能量，少量充能层数并全部消耗；>6:获得1离子充能球\n" +
                "7.  无我良秀改动：\n" +
                "   I  无我良秀将    去，也  我    伤害上    低\n" +
                "   II 无我  超过300直接使用        , 也      去\n" +
                "8.  瓦伦希娜调整：\n" +
                "   I  瓦伦希娜在预知眼>6的情况下受到直接伤害将会消耗6预知眼闪避本次伤害\n" +
                "   II 瓦伦希娜的本回合迅捷bug太多了！所以移除了\n" +
                "9.  所以有人还记得这本来应该是个指令终端么？\n" +
                "10. 新卡：第六感[代码：sixthSense](1):获得一定闪避和敏捷\n" +
                "11. 新卡：脑叶公司E.G.O::魔弹[代码：EGOMagicBullet](0)", announces);

        addAnnounce("v0.2改 小更新\n" +
                "修复已知bug/调整：\n" +
                "1. 修复了连续射击和调用连续射击的处置产生死循环的bug\n" +
                "2. 里恩沉沦过高，叠起来后可能直接每回合都使玩家混乱，故而怒削之[绝对不是我被打死档了]\n" +
                "3. 修复了魔弹不消耗子弹和第七弹-绝望会递归导致瞬秒自己的问题\n" +
                "4. 新的卡牌出现:疯狂の蛇(绝命巳乱)[代码：Si](2):根据自身呼吸法强度重复攻击数次，最少3次，每次攻击均会施加破裂强度和层数，然后根据其强度和层数施加剧毒\n" +
                "5. @全体黑兽 ，收到扣三技能 现在可以印出疯狂の蛇(绝命巳乱)了，这算平调(?)，破裂队，你崛起吧！\n" +
                "6. 比起开·道，闪开，老子自己来似乎有点牢了，于是加强了其伤害等数值，并且为其施加了新时代卡牌特有的重放\n" +
                "7. 里恩增加部分悬浮文本，如\"无我梦中\", 这里里恩真是我最用心做的了，但还是有待完善，一个人的抛瓦真的不够\n" +
                "8. 准备制作克罗默/欲成原初的克罗默, 机制预告：\n" +
                "   I   克罗默会大量施加流血，并且使用净化时会根据目标身上的流血重投本技能，无上限\n" +
                "   II  克罗默还会使用神秘の哨声在本次战斗内削弱玩家并增强自身，但神秘の哨声不会施加流血层数，如果...?\n" +
                "9. 准备制作神\n" +
                "   I   神会通过基础技能削减自己的理智\n" +
                "   II  当神受到攻击时会为自己施加沉沦\n" +
                "   III 当神的理智降低至-45，应该不会有什么不好的事情发生的...吧？（憋笑）\n", announces);

        addAnnounce("v0.3更新\n" +
                "修复的bug/调整：\n" +
                "1. 我很好奇我为什么要边听Yesod和Hod的核心抑制音乐边写代码\n" +
                "2. 感觉Hod的核心抑制音乐真阳间，只是听完后感觉敲不动键盘了\n" +
                "3. 扯远了，总之现在画的饼已经完成了一部分了，我们的伞神的逻辑已经全部写好了，如下：\n" +
                "   I   受到攻击时，根据自身沉沦层数获得{0}0%的减伤，不超过50%\n" +
                "   II  受到攻击时，获得+1沉沦层数和+1沉沦强度, 且伞神技能具有大量自沉沦施加\n" +
                "   III 剩下的机制不足道矣了，总之，伞神就是一个纯纯的沙包，相信我\n" +
                "4. 克罗默还在等待，懒得做了是这样的\n" +
                "5. 修复危险度设置输入框提示是加护的bug\n" +
                "6. 提醒各位，以下行为会导致存档的消失\n" +
                "   I   设置界面的将你抹去，也将我抹去\n" +
                "   II  设置界面作弊码填入/kill\n" +
                "   III 删除并重新安装本软件\n" +
                "7. 一些好玩的卡以及其对应的代码，可以用getCard作弊码获取\n" +
                "   1. cogito是卡牌Cogito的识别码，Cogito是个十分有趣的牌，可以试试卡组只带2张Cogito然后碰运气\n" +
                "   2. WordPower_Death是言灵【死】的识别码，不会吧不会吧，不会到现在都有人不知道秒杀的牌的获取方式吧\n" +
                "   3. @AllHeiShou是@全体黑兽，收到扣三技能的识别码，未来其召唤的黑兽种类会更多\n" +
                "   4. setDanger是危险度设置的识别码，可以用于设置危险度，不会有人不知道这张牌吧？\n" +
                "   5. 作弊码填入getCard后确认会有弹窗让你输入卡牌识别码，注意，识别码区分大小写\n", announces);

        TextView tvAnnounce = findViewById(R.id.Announcement);
        refreshUI(tvAnnounce, announces, currentIndex);

        findViewById(R.id.done).setOnClickListener(v ->
                startActivity(new Intent(this, SettingActivity.class))
        );

        findViewById(R.id.toNext).setOnClickListener(v -> {
            if (currentIndex <= 0) {
                Toast.makeText(this, "没有更新的公告啦！", Toast.LENGTH_SHORT).show();
            } else {
                currentIndex--;
                refreshUI(tvAnnounce, announces, currentIndex);
            }
        });

        findViewById(R.id.toPrevious).setOnClickListener(v -> {
            if (currentIndex >= announces.size() - 1) {
                Toast.makeText(this, "没有更旧的公告啦！", Toast.LENGTH_SHORT).show();
            } else {
                currentIndex++;
                refreshUI(tvAnnounce, announces, currentIndex);
            }
        });
    }

    public void refreshUI(TextView area, ArrayList<String> announces, int index) {
        area.setText(announces.get(index));
    }
}