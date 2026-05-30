-- MVP 种子数据：章节、翻译题、示例视频及句轴

INSERT INTO reading_chapter (title, slug, summary, content_html, sort_order, is_vip, status) VALUES
('阅读技巧：先看题干再读文章', 'skim-questions-first',
 '四六级阅读时间紧，学会带着问题读文章。',
 '<h2>核心方法</h2><p>1. 先浏览题干，圈出关键词。<br>2. 回文定位，同义替换是考点。<br>3. 不确定时排除明显错误项。</p><p><em>模拟练习请见后续章节。</em></p>',
 1, 0, 1),
('阅读技巧：长难句拆解', 'sentence-splitting',
 '面对长难句，先找主干再解修饰。',
 '<h2>三步拆解法</h2><p>找谓语 → 找主语 → 剥离从句与插入语。</p><p>每天坚持分析 3 个长难句，阅读速度会明显提升。</p>',
 2, 0, 1);

INSERT INTO translation_chapter (title, slug, summary, content_html, sort_order, is_vip, status) VALUES
('翻译技巧：中文定语的英文处理', 'modifier-handling',
 '中文多短句修饰，英文多用从句或介词短语。',
 '<h2>要点</h2><p>短修饰 → 前置形容词；长修饰 → 后置定语从句或分词结构。</p>',
 1, 0, 1),
('翻译技巧：四字格与成语', 'idiom-translation',
 '成语不要逐字硬译，找英文习惯表达。',
 '<h2>示例思路</h2><p>「人山人海」→ a sea of people / huge crowds</p>',
 2, 1, 1);

INSERT INTO translation_question (chapter_id, direction, prompt_zh, reference_answer, difficulty, is_mock, is_vip, sort_order, status) VALUES
(1, 'zh2en',
 '随着经济的快速发展，越来越多的人选择在大城市工作和生活。',
 'With the rapid development of the economy, more and more people choose to live and work in big cities.',
 'cet4', 1, 0, 1, 1);

INSERT INTO video_series (title, description, sort_order, status) VALUES
('ISLAND 精选', '四六级岛双语学习首发系列', 1, 1);

INSERT INTO video (series_id, title, description, storage_type, provider, source_url, embed_bvid, duration_sec, difficulty, is_vip, sort_order, license_note, status) VALUES
(1, 'Why Should You Read Edgar Allan Poe',
 'TED-Ed 动画：为什么应该读爱伦·坡', 'embed', 'bilibili',
 'https://www.bilibili.com/video/BV1qP4y1M7cb/', 'BV1qP4y1M7cb', 300, 'medium', 0, 1,
 'B站Embed；TED-Ed 非商用学习', 1),
(1, '你的手机是老虎机？科技如何偷走你的时间',
 'TED 演讲：数字产品设计与注意力', 'embed', 'bilibili',
 'https://www.bilibili.com/video/BV1WhX6YbEWe/', 'BV1WhX6YbEWe', 840, 'medium', 0, 2,
 'B站Embed；标注转自 TED', 1),
(1, '乔布斯斯坦福大学毕业典礼演讲',
 'Stay hungry, Stay foolish 经典演讲', 'embed', 'bilibili',
 'https://www.bilibili.com/video/BV1oW411h7Ea/', 'BV1oW411h7Ea', 900, 'medium', 0, 3,
 'B站Embed；公开演讲学习用途', 1);

INSERT INTO video_sentence (video_id, seq, start_ms, end_ms, text_en, text_zh) VALUES
(1, 1, 0, 4200, 'The poet Edgar Allan Poe has long been associated with dark and mysterious tales.',
 '诗人埃德加·爱伦·坡长期以来都与黑暗神秘的故事联系在一起。'),
(1, 2, 4200, 8800, 'But why should you read him today?',
 '但为什么今天仍要读他的作品？'),
(1, 3, 8800, 14500, 'Poe helped invent the modern detective story and science fiction.',
 '坡帮助开创了现代侦探小说和科幻小说。'),
(1, 4, 14500, 21000, 'His writing explores the depths of human psychology and fear.',
 '他的作品探索了人类心理与恐惧的深处。'),
(1, 5, 21000, 28000, 'Reading Poe trains you to notice language, rhythm, and suspense.',
 '阅读坡的作品能训练你对语言、节奏和悬念的敏感度。');
