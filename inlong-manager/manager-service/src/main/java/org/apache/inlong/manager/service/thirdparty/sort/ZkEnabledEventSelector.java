/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.thirdparty.sort;

import org.apache.inlong.manager.common.enums.Constant;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.EventSelector;
import org.springframework.stereotype.Component;

/**
 * Event selector for whether ZooKeeper is enabled.
 */
@Component
public class ZkEnabledEventSelector implements EventSelector {

    @Override
    public boolean accept(WorkflowContext context) {
        ProcessForm processForm = context.getProcessForm();
        if (!(processForm instanceof GroupResourceProcessForm)) {
            return false;
        }
        GroupResourceProcessForm groupResourceForm = (GroupResourceProcessForm) processForm;
        InlongGroupInfo groupInfo = groupResourceForm.getGroupInfo();
        return groupInfo.getZookeeperEnabled() == 1 && !groupInfo.getMiddlewareType().equals(Constant.MIDDLEWARE_NONE);
    }

}
