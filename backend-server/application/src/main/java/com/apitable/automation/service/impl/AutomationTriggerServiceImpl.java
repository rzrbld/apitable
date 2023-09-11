/*
 * APITable <https://github.com/apitable/apitable>
 * Copyright (C) 2022 APITable Ltd. <https://apitable.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.apitable.automation.service.impl;

import static java.util.stream.Collectors.toList;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.apitable.automation.entity.AutomationTriggerEntity;
import com.apitable.automation.mapper.AutomationTriggerMapper;
import com.apitable.automation.model.AutomationTriggerDto;
import com.apitable.automation.model.TriggerCopyResultDto;
import com.apitable.automation.service.IAutomationTriggerService;
import com.apitable.shared.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AutomationTriggerServiceImpl implements IAutomationTriggerService {

    @Resource
    private AutomationTriggerMapper triggerMapper;

    @Override
    public List<AutomationTriggerDto> getTriggersByRobotIds(List<String> robotIds) {
        return triggerMapper.selectTriggersByRobotIds(robotIds);
    }

    @Override
    public void create(AutomationTriggerEntity entity) {
        triggerMapper.insert(entity);
    }

    @Override
    public TriggerCopyResultDto copy(Long userId, boolean sameSpace,
        Map<String, String> newRobotMap, Map<String, String> newNodeMap) {
        List<AutomationTriggerEntity> triggers =
            triggerMapper.selectByRobotIds(newRobotMap.keySet());
        if (CollUtil.isEmpty(triggers)) {
            return new TriggerCopyResultDto();
        }
        Map<String, String> newTriggerMap = triggers.stream()
            .collect(Collectors.toMap(AutomationTriggerEntity::getTriggerId,
                i -> IdUtil.createAutomationTriggerId()));
        List<AutomationTriggerEntity> entities = new ArrayList<>(triggers.size());
        for (AutomationTriggerEntity trigger : triggers) {
            AutomationTriggerEntity entity = AutomationTriggerEntity.builder()
                .id(IdWorker.getId())
                .robotId(newRobotMap.get(trigger.getRobotId()))
                .triggerTypeId(trigger.getTriggerTypeId())
                .triggerId(newTriggerMap.get(trigger.getTriggerId()))
                .input(trigger.getInput())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
            if (trigger.getPrevTriggerId() != null) {
                entity.setPrevTriggerId(newTriggerMap.get(trigger.getPrevTriggerId()));
            }
            if (StrUtil.isNotBlank(trigger.getResourceId())) {
                String newNodeId = sameSpace ? trigger.getResourceId() :
                    Optional.ofNullable(newNodeMap.get(trigger.getResourceId()))
                        .orElse(StrUtil.EMPTY);
                String input = sameSpace ? trigger.getInput() :
                    trigger.getInput().replace(trigger.getResourceId(), newNodeId);
                entity.setResourceId(newNodeId);
                entity.setInput(input);
            } else {
                entity.setResourceId(StrUtil.EMPTY);
            }
            entities.add(entity);
        }
        triggerMapper.insertList(entities);
        Map<String, List<String>> robotIdToTriggerIdsMap =
            triggers.stream().collect(Collectors.groupingBy(AutomationTriggerEntity::getRobotId,
                Collectors.mapping(AutomationTriggerEntity::getTriggerId, toList())));
        return new TriggerCopyResultDto(robotIdToTriggerIdsMap, newTriggerMap);
    }

    @Override
    public void updateByTriggerId(AutomationTriggerEntity trigger) {
        triggerMapper.updateByTriggerId(trigger.getTriggerId(),
            trigger.getTriggerTypeId(), trigger.getInput());
    }

}
