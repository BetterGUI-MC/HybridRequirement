package me.hsgamer.bettergui.hybridrequirement;

import me.hsgamer.bettergui.api.menu.Menu;
import me.hsgamer.bettergui.api.process.ProcessApplier;
import me.hsgamer.bettergui.api.requirement.Requirement;
import me.hsgamer.bettergui.builder.RequirementBuilder;
import me.hsgamer.bettergui.requirement.RequirementSet;
import me.hsgamer.hscore.common.MapUtils;
import me.hsgamer.hscore.common.Validate;
import me.hsgamer.hscore.task.element.TaskPool;

import java.util.*;

public class HybridRequirement implements Requirement {
    private final String name;
    private final Menu menu;
    private final boolean successOnly;
    private final int minimum;
    private final List<SetObject> setList;

    public HybridRequirement(RequirementBuilder.Input input) {
        this.name = input.name;
        this.menu = input.menu;

        Map<String, Object> valueMap = MapUtils.castOptionalStringObjectMap(input.value).orElseGet(Collections::emptyMap);
        boolean successOnly = false;
        int minimum = -1;
        List<SetObject> setList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equalsIgnoreCase("success-only")) {
                successOnly = Boolean.parseBoolean(String.valueOf(value));
            } else if (key.equalsIgnoreCase("minimum")) {
                minimum = Validate.getNumber(String.valueOf(value)).map(Number::intValue).orElse(minimum);
            } else if (value instanceof Map) {
                Map<String, Object> map = MapUtils.castOptionalStringObjectMap(value).orElseGet(Collections::emptyMap);
                RequirementSet set = new RequirementSet(menu, getName() + "_" + key, map);
                boolean optional = false;
                if (map.containsKey("optional")) {
                    optional = Boolean.parseBoolean(String.valueOf(map.get("optional")));
                }
                setList.add(new SetObject(set, optional));
            }
        }

        this.successOnly = successOnly;
        this.minimum = minimum;
        this.setList = setList;
    }

    @Override
    public Result check(UUID uuid) {
        int success = 0;
        int total = setList.size();
        List<ProcessApplier> processAppliers = new ArrayList<>();
        for (SetObject setObject : setList) {
            RequirementSet set = setObject.set;
            Requirement.Result result = set.check(uuid);
            if (result.isSuccess) {
                success++;
            }

            if (setObject.optional) {
                continue;
            }

            if (!successOnly || result.isSuccess) {
                processAppliers.add(result.applier);
            }
        }

        boolean isSuccess;
        if (minimum < 0) {
            isSuccess = success == total;
        } else {
            isSuccess = success >= minimum;
        }

        return new Result(isSuccess, (uuid1, taskProcess) -> {
            TaskPool taskPool = taskProcess.getCurrentTaskPool();
            processAppliers.forEach(processApplier -> taskPool.addLast(() -> processApplier.accept(uuid1, taskProcess)));
            taskProcess.next();
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Menu getMenu() {
        return menu;
    }

    private static class SetObject {
        private final RequirementSet set;
        private final boolean optional;

        private SetObject(RequirementSet set, boolean optional) {
            this.set = set;
            this.optional = optional;
        }
    }
}
