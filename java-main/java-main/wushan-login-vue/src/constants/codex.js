// 宝库典藏常量 —— 功法 / 心得 / 修炼资源 / 灵根 / 秘境
// 品阶由低到高：凡品 → 黄品 → 玄品 → 地品 → 天品 → 圣品
// 数据结构统一，便于 api/codex.js 过滤，也便于后端建表对照。

/* ------------------------------ 品阶 ------------------------------ */
export const GRADES = [
  { key: 'fan', label: '凡品', color: '#9aa08c' },
  { key: 'huang', label: '黄品', color: '#e8c372' },
  { key: 'xuan', label: '玄品', color: '#3a8fd0' },
  { key: 'di', label: '地品', color: '#9d7bf0' },
  { key: 'tian', label: '天品', color: '#e8412b' },
  { key: 'sheng', label: '圣品', color: '#f2e34d' }
]

export function findGrade(key) {
  return GRADES.find((g) => g.key === key) || GRADES[0]
}

/* ------------------------------ 分类 ------------------------------ */
export const CATEGORIES = [
  { key: 'gongfa', label: '功法', en: 'technique' },
  { key: 'xinde', label: '心得', en: 'insight' },
  { key: 'ziyuan', label: '修炼资源', en: 'resource' }
]

/* ------------------------------ 条目（功法 / 心得 / 资源） ------------------------------ */
export const CODEX_ITEMS = [
  /* ---- 火 ---- */
  { id: 'huo-gf-1', element: 'huo', category: 'gongfa', grade: 'tian', title: '焚天七绝焰', desc: '七式连环，焰随心走，至刚至烈，火行至高功法。' },
  { id: 'huo-gf-2', element: 'huo', category: 'gongfa', grade: 'di', title: '心火燎原功', desc: '以内火淬炼经脉，痛而后明，进境极快。' },
  { id: 'huo-gf-3', element: 'huo', category: 'gongfa', grade: 'xuan', title: '引火诀', desc: '引外火为己用，火行入门之基，稳而无毒。' },
  { id: 'huo-xd-1', element: 'huo', category: 'xinde', grade: 'huang', title: '《火候记》', desc: '文火武火，全凭一心，论火行修炼的节奏。' },
  { id: 'huo-xd-2', element: 'huo', category: 'xinde', grade: 'di', title: '《明心一炬》', desc: '火不照外物，照的是本心，心明则焰清。' },
  { id: 'huo-zy-1', element: 'huo', category: 'ziyuan', grade: 'tian', title: '地心火髓', desc: '地脉深处的火之精华，炼体炼器两相宜。' },
  { id: 'huo-zy-2', element: 'huo', category: 'ziyuan', grade: 'xuan', title: '朱果', desc: '赤红果实，服之火行亲和大增。' },
  { id: 'huo-zy-3', element: 'huo', category: 'ziyuan', grade: 'huang', title: '火纹石', desc: '石纹如焰，铺设火阵的常用材料。' },
  /* ---- 金 ---- */
  { id: 'jin-gf-1', element: 'jin', category: 'gongfa', grade: 'tian', title: '庚金百斩诀', desc: '百斩一意，斩物斩念，金行至高攻伐之法。' },
  { id: 'jin-gf-2', element: 'jin', category: 'gongfa', grade: 'di', title: '金刚不坏体', desc: '淬体如铁，横练金身，守御第一。' },
  { id: 'jin-gf-3', element: 'jin', category: 'gongfa', grade: 'xuan', title: '锐金剑指', desc: '指出如剑，锋锐凝于一线，破甲穿石。' },
  { id: 'jin-xd-1', element: 'jin', category: 'xinde', grade: 'huang', title: '《断金录》', desc: '当断不断，反受其乱，论金行之决断。' },
  { id: 'jin-xd-2', element: 'jin', category: 'xinde', grade: 'di', title: '《锋藏于鞘》', desc: '利刃在鞘方为利刃，藏锋是金行的第一课。' },
  { id: 'jin-zy-1', element: 'jin', category: 'ziyuan', grade: 'tian', title: '庚金精', desc: '万年金气凝成的精粒，锻造上品兵刃之基。' },
  { id: 'jin-zy-2', element: 'jin', category: 'ziyuan', grade: 'xuan', title: '白玉髓', desc: '温润如玉而坚逾铁，炼器辅材。' },
  { id: 'jin-zy-3', element: 'jin', category: 'ziyuan', grade: 'huang', title: '剑心草', desc: '叶如剑形，服之坚定剑心，不易动摇。' },
  /* ---- 木 ---- */
  { id: 'mu-gf-1', element: 'mu', category: 'gongfa', grade: 'tian', title: '青木长生功', desc: '生生不息，伤损自愈，木行至高养生法。' },
  { id: 'mu-gf-2', element: 'mu', category: 'gongfa', grade: 'di', title: '万藤缠身手', desc: '柔藤千变，缠困绞锁，以柔克刚。' },
  { id: 'mu-gf-3', element: 'mu', category: 'gongfa', grade: 'xuan', title: '木灵吐纳法', desc: '晨昏吐纳，与草木共鸣，固本培元。' },
  { id: 'mu-xd-1', element: 'mu', category: 'xinde', grade: 'huang', title: '《本草问答》', desc: '以问答体论山中药性，采药必读。' },
  { id: 'mu-xd-2', element: 'mu', category: 'xinde', grade: 'di', title: '《生生不息论》', desc: '损而不折，折而复生，木行的大道。' },
  { id: 'mu-zy-1', element: 'mu', category: 'ziyuan', grade: 'tian', title: '千年灵芝', desc: '固本培元第一灵药，重伤亦可续命。' },
  { id: 'mu-zy-2', element: 'mu', category: 'ziyuan', grade: 'xuan', title: '木灵乳', desc: '古树流出的乳白津液，温养经脉。' },
  { id: 'mu-zy-3', element: 'mu', category: 'ziyuan', grade: 'huang', title: '固元参', desc: '常见的培元参类，价平效稳。' },
  /* ---- 水 ---- */
  { id: 'shui-gf-1', element: 'shui', category: 'gongfa', grade: 'tian', title: '沧浪十七叠', desc: '后浪推前浪，十七叠力，水行至高攻法。' },
  { id: 'shui-gf-2', element: 'shui', category: 'gongfa', grade: 'di', title: '弱水诀', desc: '至柔至韧，滴水穿石，绵长不休。' },
  { id: 'shui-gf-3', element: 'shui', category: 'gongfa', grade: 'xuan', title: '凝冰指', desc: '点水成冰，寒气侵体，迟滞敌形。' },
  { id: 'shui-xd-1', element: 'shui', category: 'xinde', grade: 'huang', title: '《观水笔记》', desc: '水无常形，随物赋形，论顺势而为。' },
  { id: 'shui-xd-2', element: 'shui', category: 'xinde', grade: 'di', title: '《静水流深》', desc: '不动的是表面，奔涌的是深处。' },
  { id: 'shui-zy-1', element: 'shui', category: 'ziyuan', grade: 'tian', title: '寒潭玉液', desc: '千年寒潭底的一捧玉液，洗经伐髓。' },
  { id: 'shui-zy-2', element: 'shui', category: 'ziyuan', grade: 'xuan', title: '水云纱', desc: '入水不濡，织衣可避水火。' },
  { id: 'shui-zy-3', element: 'shui', category: 'ziyuan', grade: 'huang', title: '净水珠', desc: '化浊为清，行路必备的珠子。' },
  /* ---- 土 ---- */
  { id: 'tu-gf-1', element: 'tu', category: 'gongfa', grade: 'tian', title: '厚土不灭体', desc: '身融大地，不动如山，土行至高守御。' },
  { id: 'tu-gf-2', element: 'tu', category: 'gongfa', grade: 'di', title: '山岳印', desc: '一印压下，重逾千钧，封地断路。' },
  { id: 'tu-gf-3', element: 'tu', category: 'gongfa', grade: 'xuan', title: '撼地桩', desc: '扎桩于地，借大地之力发劲。' },
  { id: 'tu-xd-1', element: 'tu', category: 'xinde', grade: 'huang', title: '《守拙篇》', desc: '大巧若拙，守得住才有以后。' },
  { id: 'tu-xd-2', element: 'tu', category: 'xinde', grade: 'di', title: '《大地无言》', desc: '土不言语，载万物而行。' },
  { id: 'tu-zy-1', element: 'tu', category: 'ziyuan', grade: 'tian', title: '息壤', desc: '生生不息的神土，一撮可成一丘。' },
  { id: 'tu-zy-2', element: 'tu', category: 'ziyuan', grade: 'xuan', title: '黄精玉髓', desc: '土中精气所结，健脾固本。' },
  { id: 'tu-zy-3', element: 'tu', category: 'ziyuan', grade: 'huang', title: '千年窑土', desc: '筑炉炼器的上好底料。' },
  /* ---- 风 ---- */
  { id: 'feng-gf-1', element: 'feng', category: 'gongfa', grade: 'tian', title: '大衍游风身法', desc: '身与风合，来去无踪，风行至高身法。' },
  { id: 'feng-gf-2', element: 'feng', category: 'gongfa', grade: 'di', title: '罡风九变', desc: '九个方位随心切换，攻守转换极快。' },
  { id: 'feng-gf-3', element: 'feng', category: 'gongfa', grade: 'xuan', title: '听风步', desc: '踏风而行，步随音动。' },
  { id: 'feng-xd-1', element: 'feng', category: 'xinde', grade: 'huang', title: '《风语集》', desc: '听风辨位，未动先知。' },
  { id: 'feng-xd-2', element: 'feng', category: 'xinde', grade: 'di', title: '《无痕之道》', desc: '风过千山，不留一物，故能过千山。' },
  { id: 'feng-zy-1', element: 'feng', category: 'ziyuan', grade: 'tian', title: '风灵羽', desc: '风灵蜕下的羽衣，御风而行的凭依。' },
  { id: 'feng-zy-2', element: 'feng', category: 'ziyuan', grade: 'xuan', title: '青岚叶', desc: '叶落而风起，练身法常用。' },
  { id: 'feng-zy-3', element: 'feng', category: 'ziyuan', grade: 'huang', title: '追风草', desc: '煎汤沐足，轻身健步。' },
  /* ---- 雷 ---- */
  { id: 'lei-gf-1', element: 'lei', category: 'gongfa', grade: 'tian', title: '九霄引雷诀', desc: '引天雷入体淬炼，雷行至高法门，凶险亦大。' },
  { id: 'lei-gf-2', element: 'lei', category: 'gongfa', grade: 'di', title: '紫霄神雷', desc: '掌心凝紫雷，一击震魂。' },
  { id: 'lei-gf-3', element: 'lei', category: 'gongfa', grade: 'xuan', title: '惊雷指', desc: '蓄而忽发，指落雷鸣。' },
  { id: 'lei-xd-1', element: 'lei', category: 'xinde', grade: 'huang', title: '《雷起于渊》', desc: '雷霆之怒，蓄于至静。' },
  { id: 'lei-xd-2', element: 'lei', category: 'xinde', grade: 'di', title: '《一线之决》', desc: '雷行修的是决断，一线之间定生死。' },
  { id: 'lei-zy-1', element: 'lei', category: 'ziyuan', grade: 'tian', title: '雷击木心', desc: '历雷不死的老木之心，镇宅辟邪。' },
  { id: 'lei-zy-2', element: 'lei', category: 'ziyuan', grade: 'xuan', title: '紫霄髓', desc: '雷云深处的凝结物，淬炼神魂。' },
  { id: 'lei-zy-3', element: 'lei', category: 'ziyuan', grade: 'huang', title: '引雷针', desc: '布阵引雷的凡铁长针。' },
  /* ---- 电 ---- */
  { id: 'dian-gf-1', element: 'dian', category: 'gongfa', grade: 'tian', title: '掣电无影剑', desc: '剑出如电，影未现而剑已收，电行至高剑法。' },
  { id: 'dian-gf-2', element: 'dian', category: 'gongfa', grade: 'di', title: '电光淬体术', desc: '以电淬髓，速度与反应俱进。' },
  { id: 'dian-gf-3', element: 'dian', category: 'gongfa', grade: 'xuan', title: '掠光诀', desc: '借光而遁，瞬息十丈。' },
  { id: 'dian-xd-1', element: 'dian', category: 'xinde', grade: 'huang', title: '《一瞬观照》', desc: '电光石火，照见本来。' },
  { id: 'dian-xd-2', element: 'dian', category: 'xinde', grade: 'di', title: '《迅烈如霹雳》', desc: '迟疑是电行唯一的天敌。' },
  { id: 'dian-zy-1', element: 'dian', category: 'ziyuan', grade: 'tian', title: '电光石髓', desc: '闪电击石所留的髓纹，服之神速。' },
  { id: 'dian-zy-2', element: 'dian', category: 'ziyuan', grade: 'xuan', title: '掣电梭', desc: '掷出即至的暗器，肉眼难追。' },
  { id: 'dian-zy-3', element: 'dian', category: 'ziyuan', grade: 'huang', title: '明澈露', desc: '洗目之露，视物极明。' },
  /* ---- 太极 ---- */
  { id: 'taiji-gf-1', element: 'taiji', category: 'gongfa', grade: 'tian', title: '太极玄功 · 周天图', desc: '周天运转，万法归一，太极至高心法。' },
  { id: 'taiji-gf-2', element: 'taiji', category: 'gongfa', grade: 'di', title: '阴阳鱼推手', desc: '听劲化劲，后发先至。' },
  { id: 'taiji-gf-3', element: 'taiji', category: 'gongfa', grade: 'xuan', title: '揽雀尾十八式', desc: '起承转合，式式圆转。' },
  { id: 'taiji-xd-1', element: 'taiji', category: 'xinde', grade: 'huang', title: '《圆转论》', desc: '至刚易折，圆转者久。' },
  { id: 'taiji-xd-2', element: 'taiji', category: 'xinde', grade: 'di', title: '《万化归一》', desc: '千招万式，尽头是一个圆。' },
  { id: 'taiji-zy-1', element: 'taiji', category: 'ziyuan', grade: 'tian', title: '太极玉璧', desc: '璧上阴阳流转，参悟圆融之理。' },
  { id: 'taiji-zy-2', element: 'taiji', category: 'ziyuan', grade: 'xuan', title: '圆转石', desc: '握之掌心自转，静心之石。' },
  { id: 'taiji-zy-3', element: 'taiji', category: 'ziyuan', grade: 'huang', title: '两仪砂', desc: '阴阳两色的细砂，布两仪阵之基。' },
  /* ---- 阴阳 ---- */
  { id: 'yinyang-gf-1', element: 'yinyang', category: 'gongfa', grade: 'tian', title: '阴阳互济功', desc: '阴阳二气互为其根，阴阳至高法门。' },
  { id: 'yinyang-gf-2', element: 'yinyang', category: 'gongfa', grade: 'di', title: '消长指', desc: '指分晦明，一指之内判阴阳。' },
  { id: 'yinyang-gf-3', element: 'yinyang', category: 'gongfa', grade: 'xuan', title: '双鱼映月诀', desc: '月下修行，映月观心。' },
  { id: 'yinyang-xd-1', element: 'yinyang', category: 'xinde', grade: 'huang', title: '《负阴抱阳》', desc: '孤阳不生，独阴不长。' },
  { id: 'yinyang-xd-2', element: 'yinyang', category: 'xinde', grade: 'di', title: '《消长之衡》', desc: '盛极必衰，衡者恒存。' },
  { id: 'yinyang-zy-1', element: 'yinyang', category: 'ziyuan', grade: 'tian', title: '阴阳双鱼佩', desc: '佩分两半，合则气机圆满。' },
  { id: 'yinyang-zy-2', element: 'yinyang', category: 'ziyuan', grade: 'xuan', title: '晦朔草', desc: '晦日朔日各开一花的奇草。' },
  { id: 'yinyang-zy-3', element: 'yinyang', category: 'ziyuan', grade: 'huang', title: '子午露', desc: '子午二时收取的露水，调和气机。' }
]

/* ------------------------------ 灵根 ------------------------------ */
export const SPIRIT_ROOTS = [
  { id: 'root-jin', element: 'jin', grade: 'tian', name: '庚金灵根', desc: '锋锐天成，习金行功法一日千里，杀伐之气重。' },
  { id: 'root-mu', element: 'mu', grade: 'tian', name: '青木灵根', desc: '生生不息，疗伤自愈远胜常人，木行天授。' },
  { id: 'root-shui', element: 'shui', grade: 'tian', name: '弱水灵根', desc: '性灵近水，吐纳如鱼得渊，变化之资。' },
  { id: 'root-huo', element: 'huo', grade: 'tian', name: '离火灵根', desc: '火毒不侵，心火自燃，攻伐之资冠绝十行。' },
  { id: 'root-tu', element: 'tu', grade: 'tian', name: '息土灵根', desc: '根骨如山，稳如磐石，守御与耐力之最。' },
  { id: 'root-feng', element: 'feng', grade: 'di', name: '罡风灵根', desc: '身轻如燕，感风先知，身法一途天赋异禀。' },
  { id: 'root-lei', element: 'lei', grade: 'di', name: '惊雷灵根', desc: '引雷淬体，怒而愈强，天生气势慑人。' },
  { id: 'root-dian', element: 'dian', grade: 'di', name: '掣电灵根', desc: '反应如电，一点即通，最忌迟疑。' },
  { id: 'root-taiji', element: 'taiji', grade: 'sheng', name: '混元灵根', desc: '万法圆融，诸系皆通，百年难出一人。' },
  { id: 'root-yinyang', element: 'yinyang', grade: 'sheng', name: '两仪灵根', desc: '阴阳同修，昼夜异能，圣品之资。' }
]

/* ------------------------------ 秘境 ------------------------------ */
export const REALMS = [
  { id: 'realm-huo-1', element: 'huo', grade: 'di', name: '焚火秘境', layers: '九层', desc: '地火奔涌，愈深愈烈，最深处传闻有火髓。' },
  { id: 'realm-huo-2', element: 'huo', grade: 'tian', name: '离火洞天', layers: '三层', desc: '天品秘境，非离火灵根者入内即灼。' },
  { id: 'realm-jin-1', element: 'jin', grade: 'di', name: '断剑冢', layers: '七层', desc: '万剑朝冢，剑气森然，剑修圣地。' },
  { id: 'realm-jin-2', element: 'jin', grade: 'xuan', name: '白刃崖', layers: '五层', desc: '崖壁如刃，采药炼体两相宜。' },
  { id: 'realm-mu-1', element: 'mu', grade: 'xuan', name: '青木林深处', layers: '五层', desc: '草木有灵，迷径重重，需持本心而行。' },
  { id: 'realm-mu-2', element: 'mu', grade: 'di', name: '长生谷', layers: '九层', desc: '药气氤氲，愈深药龄愈老。' },
  { id: 'realm-shui-1', element: 'shui', grade: 'xuan', name: '寒潭秘境', layers: '三层', desc: '潭底别有洞天，寒气刺骨。' },
  { id: 'realm-shui-2', element: 'shui', grade: 'tian', name: '沧浪海眼', layers: '一层', desc: '海眼之下暗流万千，一步一世界。' },
  { id: 'realm-tu-1', element: 'tu', grade: 'di', name: '地肺石窟', layers: '十二层', desc: '层峦入地，矿脉纵横，土行宝藏。' },
  { id: 'realm-tu-2', element: 'tu', grade: 'tian', name: '息壤秘窟', layers: '一层', desc: '天品秘境，见者有份，去者多留。' },
  { id: 'realm-feng-1', element: 'feng', grade: 'xuan', name: '风眼长廊', layers: '九层', desc: '罡风穿廊，身法试炼之地。' },
  { id: 'realm-feng-2', element: 'feng', grade: 'di', name: '青岚绝壁', layers: '五层', desc: '岚气缠壁，攀者如御风。' },
  { id: 'realm-lei-1', element: 'lei', grade: 'di', name: '落雷原', layers: '七层', desc: '终年雷暴，引雷淬体者的乐园。' },
  { id: 'realm-lei-2', element: 'lei', grade: 'tian', name: '九霄雷池', layers: '一层', desc: '天品秘境，雷池之水可淬神魂。' },
  { id: 'realm-dian-1', element: 'dian', grade: 'xuan', name: '电光裂缝', layers: '三层', desc: '裂缝间电光游走，进退须快。' },
  { id: 'realm-dian-2', element: 'dian', grade: 'di', name: '掣电之隙', layers: '一层', desc: '一瞬即达，一瞬即失，考验眼力。' },
  { id: 'realm-taiji-1', element: 'taiji', grade: 'tian', name: '太极幻境', layers: '一层', desc: '阴阳双鱼流转，入境者各见本心。' },
  { id: 'realm-yinyang-1', element: 'yinyang', grade: 'di', name: '晦明谷', layers: '三层', desc: '昼夜各半，阴阳消长，修炼互济功之地。' }
]
