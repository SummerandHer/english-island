package com.island.module.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.island.module.video.VideoTagRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VideoTagRelMapper extends BaseMapper<VideoTagRel> {

	@Select("""
			SELECT DISTINCT r.video_id
			FROM video_tag_rel r
			INNER JOIN video_tag t ON t.id = r.tag_id AND t.status = 1
			WHERE t.slug = #{slug}
			""")
	List<Long> listVideoIdsByTagSlug(@Param("slug") String slug);
}
