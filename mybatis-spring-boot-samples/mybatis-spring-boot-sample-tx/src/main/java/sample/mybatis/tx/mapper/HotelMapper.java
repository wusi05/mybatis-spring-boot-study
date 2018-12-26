package sample.mybatis.tx.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import sample.mybatis.tx.domain.Hotel;
import sample.mybatis.tx.domain.HotelExample;

public interface HotelMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    int countByExample(HotelExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    int deleteByExample(HotelExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    int insert(Hotel record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    int insertSelective(Hotel record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    List<Hotel> selectByExample(HotelExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    int updateByExampleSelective(@Param("record") Hotel record, @Param("example") HotelExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_hotel
     *
     * @mbggenerated
     */
    int updateByExample(@Param("record") Hotel record, @Param("example") HotelExample example);
}