package com.island.module.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.island.module.video.VideoSentence;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VideoSentenceMapper extends BaseMapper<VideoSentence> {

	@Select("""
			<script>
			SELECT video_id AS videoId, COUNT(*) AS cnt
			FROM video_sentence
			WHERE video_id IN
			<foreach collection="videoIds" item="id" open="(" separator="," close=")">
			  #{id}
			</foreach>
			GROUP BY video_id
			</script>
			""")
	List<SentenceCountRow> countByVideoIds(@Param("videoIds") List<Long> videoIds);

	record SentenceCountRow(Long videoId, int cnt) {}
}
