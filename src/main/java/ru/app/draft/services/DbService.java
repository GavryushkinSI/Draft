package ru.app.draft.services;

import liquibase.pro.packaged.I;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.app.draft.models.Strategy;
import ru.app.draft.models.User;
import ru.app.draft.models.UserCache;
import ru.app.draft.repository.StrategyRepository;
import ru.app.draft.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ru.app.draft.store.Store.USER_STORE;

@Service
public class DbService {

    private final UserRepository userRepository;
    private final StrategyRepository strategyRepository;

    public DbService(UserRepository userRepository, StrategyRepository strategyRepository) {
        this.userRepository = userRepository;
        this.strategyRepository = strategyRepository;
    }

    @Transactional
    public void saveUsers(List<UserCache> list) {
        Random random = new Random();
        list.forEach(i -> {
            User userDto = i.getUser();
            ru.app.draft.entity.User userEntity = new ru.app.draft.entity.User();
            userEntity.setId(random.nextLong());
            userEntity.setChartid(userDto.getChatId());
            userEntity.setEmail(userDto.getEmail());
            userEntity.setLogin(userDto.getLogin());
            userEntity.setPassword(userDto.getPassword());
            ru.app.draft.entity.User userSaved = userRepository.save(userEntity);
            List<Strategy> strategyDto = i.getStrategies();
            List<ru.app.draft.entity.Strategy> strategyEntity = new ArrayList<>(strategyDto.size());
            strategyDto.forEach(s -> {
                ru.app.draft.entity.Strategy strategy = new ru.app.draft.entity.Strategy();
                strategy.setActive(s.getIsActive());
                strategy.setUsers(userSaved);
                strategy.setId(Long.valueOf(s.getId()));
                strategy.setDirection(s.getDirection());
                strategy.setName(s.getName());
                strategy.setTicker(s.getTicker());
                strategy.setFigi(s.getFigi());
                strategy.setPosition(Math.toIntExact(s.getCurrentPosition()));
                strategyEntity.add(strategy);
            });
            strategyRepository.saveAll(strategyEntity);
        });
    }

    @Transactional
    public void getAllUsers() {
        List<ru.app.draft.entity.User> entities = userRepository.findAll();
        entities.forEach(i -> {
            User user = new User(i.getLogin(), i.getPassword(), i.getEmail(), i.getChartid());
            USER_STORE.put(user.getLogin(), new UserCache(user));
        });
    }
}
