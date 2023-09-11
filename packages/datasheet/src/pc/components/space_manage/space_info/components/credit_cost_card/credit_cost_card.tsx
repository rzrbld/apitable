import classnames from 'classnames';
import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import useSWR from 'swr';
import urlcat from 'urlcat';
import { DropdownSelect, IOption, Typography, useThemeColors } from '@apitable/components';
import { TextLabel } from 'pc/components/space_manage/space_info/components/credit_cost_card/components/text_label';
import { GET_CREDIT_STATISTICS, SELECT_LIST, TimeDimension } from 'pc/components/space_manage/space_info/components/credit_cost_card/enum';
import { convertDate } from 'pc/components/space_manage/space_info/components/credit_cost_card/utils/convert_date';
import { formatDate } from 'pc/components/space_manage/space_info/components/credit_cost_card/utils/date';
import { getCreditStatisticsFetcher } from 'pc/components/space_manage/space_info/components/credit_cost_card/utils/fetcher';
import { CardTitle } from 'pc/components/space_manage/space_info/ui';
import styles from './style.module.less';

interface ICreditCostCardProps {
  minHeight?: string | number;
  className?: string;
  title: string;
  titleTip: string;
  strokeColor: string;
}

export const CreditCostCard: React.FC<ICreditCostCardProps> = ({ strokeColor, minHeight, className, titleTip, ...props }) => {
  const color = useThemeColors();
  const spaceId = useSelector((state) => state.space.activeId);
  const [timeDimension, setTimeDimension] = useState(TimeDimension.WEEKDAY);

  const { data } = useSWR(urlcat(GET_CREDIT_STATISTICS, { spaceId }) + `?timeDimension=${timeDimension}`, getCreditStatisticsFetcher);
  const onSelected = (option: IOption) => {
    setTimeDimension(option.value as TimeDimension);
  };

  const _data = convertDate(timeDimension, data);
  return (
    <div className={classnames(styles.card, 'vk-flex vk-flex-col vk-space-y-4 vk-p-4', className)} style={{ minHeight }}>
      <CardTitle
        {...props}
        tipTitle={titleTip}
        rightSlot={
          <div className={'vk-flex vk-items-center vk-space-x-1'}>
            <Typography variant={'body3'} color={color.textCommonTertiary}>
              Time
            </Typography>
            <DropdownSelect triggerStyle={{ height: '32px', width: '120px' }} options={SELECT_LIST} onSelected={onSelected} value={timeDimension} />
          </div>
        }
      />
      <div style={{ height: 'calc(100% - 38px)' }}>
        {/*{!_data.length && (*/}
        {/*  <div className={'vk-absolute vk-end-0 vk-top-0 vk-flex vk-w-full vk-items-center vk-justify-center'}>*/}
        {/*    <Typography variant={'h7'}>暂无数据</Typography>*/}
        {/*  </div>*/}
        {/*)}*/}
        <ResponsiveContainer width="100%" height="100%" debounce={1000}>
          <LineChart
            width={500}
            height={300}
            data={_data}
            margin={{
              top: 5,
              right: 30,
              left: -15,
              bottom: 5,
            }}
          >
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={color.borderCommonDefault} />
            {/*<XAxis dataKey="name" />*/}
            <XAxis
              dataKey="dateline"
              tick={{ stroke: color.textCommonPrimary, strokeWidth: 0.5 }}
              // tickLine={{ stroke: 'red' }}
              // tickFormatter={(timeStr) => {
              //   return dayjs(timeStr).format('YYYY-MM-DD');
              // }}
              stroke={color.borderCommonDefault}
            />
            <YAxis domain={[0, 'dataMax + 5']} tick={{ stroke: color.textCommonPrimary, strokeWidth: 0.5 }} stroke={color.borderCommonDefault} />
            <Tooltip
              labelStyle={{ color: color.textReverseDefault }}
              itemStyle={{ color: color.textReverseDefault }}
              contentStyle={{ background: color.bgReverseDefault }}
              cursor={false}
              // cursor={<CustomCursor />}
              // viewBox={{ x:110, y: 111, width: 400, height: 400 }}
              labelFormatter={(str) => formatDate(timeDimension, str)}
            />
            {/*<Legend />*/}
            <Line
              type="linear"
              dataKey="credit"
              stroke={strokeColor}
              strokeWidth={3}
              activeDot={{ r: 4 }}
              label={<TextLabel color={color.textCommonPrimary} />}
              dot={{ stroke: strokeColor, strokeWidth: 1 }}
              // activeDot={false}
            />
            {/*<Line type="linear" dataKey="uv" stroke="#82ca9d"/>*/}
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};
