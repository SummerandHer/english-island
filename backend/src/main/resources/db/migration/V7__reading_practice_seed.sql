-- Phase 2.5: 阅读模拟题种子（2 篇章 × 4 题，自编模拟 is_mock=1）

INSERT INTO reading_passage (chapter_id, title, content_en, word_count, difficulty, is_mock, source_id, sort_order, status) VALUES
(1, '模拟练习：带着问题读城市生活',
'More college students are choosing to live in big cities after graduation. Before reading a passage like this in the CET exam, you should preview the questions and underline keywords such as "students," "cities," and "reasons." While reading, look for synonyms instead of identical words. For example, "rapid growth" may appear as "fast expansion" in the text.

Researchers in Nanjing surveyed 2,400 graduates and found that career opportunities were the top reason for staying in metropolitan areas. About 58 percent mentioned access to training programs, while only 12 percent said nightlife was important. The report also notes that housing costs remain the biggest challenge, especially for those who do not receive financial support from their parents.

When an option sounds too general or uses absolute words like "all" or "never," be cautious. The correct answer usually restates the same meaning in different words. Practicing this habit for ten minutes a day can save you valuable time during the real test.',
268, 'cet4', 1, 1, 1, 1),
(2, '模拟练习：长难句与科技报道',
'Scientists who study artificial intelligence often publish results that surprise the public, especially when machines perform better than expected on language tasks. To understand sentences like this one, first locate the main verb: "publish." The subject is "Scientists," but it is followed by a long modifier "who study artificial intelligence."

A recent paper from a Beijing laboratory explains that large models can summarize news articles in seconds. However, the researchers warn that these systems may invent facts if they are not checked by humans. The team tested 500 articles and discovered errors in 7 percent of the summaries, particularly in numbers and dates.

Another challenge is energy use. Training a single advanced model may consume as much electricity as dozens of households use in a year. Because of this issue, some universities are teaching students to evaluate AI tools critically rather than accept every answer automatically.',
248, 'cet4', 1, 1, 1, 1);

-- Passage 1 questions (passage id = 1)
INSERT INTO reading_question (passage_id, question_type, stem, explanation, sort_order) VALUES
(1, 'single', 'What should students do before reading the passage in the exam?', '题干关键词 preview the questions 与首段 advice 对应。', 1),
(1, 'single', 'According to the survey, why do most graduates stay in big cities?', '文中 58% 与 career opportunities 对应，注意同义替换。', 2),
(1, 'single', 'What is mentioned as the biggest challenge for young people in cities?', '末段 housing costs 为直接信息。', 3),
(1, 'single', 'What does the author suggest about answer choices containing "all" or "never"?', '文中 be cautious 与选项 be careful 同义。', 4);

INSERT INTO reading_question_option (question_id, label, content, is_correct) VALUES
(1, 'A', 'Memorize every word in the passage', 0),
(1, 'B', 'Preview the questions and mark keywords', 1),
(1, 'C', 'Read the passage three times slowly', 0),
(1, 'D', 'Translate each sentence into Chinese first', 0),
(2, 'A', 'Cheaper housing prices', 0),
(2, 'B', 'Career opportunities and training', 1),
(2, 'C', 'Nightlife and entertainment', 0),
(2, 'D', 'Shorter working hours', 0),
(3, 'A', 'Finding roommates', 0),
(3, 'B', 'Learning foreign languages', 0),
(3, 'C', 'Housing costs', 1),
(3, 'D', 'Lack of public transport', 0),
(4, 'A', 'They are always correct', 0),
(4, 'B', 'They should be chosen first', 0),
(4, 'C', 'They are usually too short', 0),
(4, 'D', 'They should be treated carefully', 1);

-- Passage 2 questions (passage id = 2)
INSERT INTO reading_question (passage_id, question_type, stem, explanation, sort_order) VALUES
(2, 'single', 'What is the main verb in the example sentence about scientists?', '定位谓语 publish。', 1),
(2, 'single', 'What problem did the Beijing laboratory find in AI summaries?', '7% 错误，尤其在 numbers and dates。', 2),
(2, 'single', 'Why are some universities changing the way they teach students about AI?', '末段 energy use 与 critically evaluate 相关。', 3),
(2, 'single', 'What does the passage imply about training advanced AI models?', '耗电高，与 households 对比。', 4);

INSERT INTO reading_question_option (question_id, label, content, is_correct) VALUES
(5, 'A', 'study', 0),
(5, 'B', 'publish', 1),
(5, 'C', 'surprise', 0),
(5, 'D', 'perform', 0),
(6, 'A', 'They are always shorter than the originals', 0),
(6, 'B', 'They may contain wrong numbers or dates', 1),
(6, 'C', 'They cannot summarize news articles', 0),
(6, 'D', 'They are illegal in China', 0),
(7, 'A', 'AI tools use too much electricity and may be unreliable', 1),
(7, 'B', 'Students prefer traditional newspapers', 0),
(7, 'C', 'Language tasks are no longer tested in exams', 0),
(7, 'D', 'Machines have replaced all teachers', 0),
(8, 'A', 'It requires little electricity', 0),
(8, 'B', 'It is cheaper than online courses', 0),
(8, 'C', 'It may use as much power as many households', 1),
(8, 'D', 'It is forbidden on campus', 0);
