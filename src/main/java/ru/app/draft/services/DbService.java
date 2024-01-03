package ru.app.draft.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.app.draft.entity.Comment;
import ru.app.draft.entity.LastPrice;
import ru.app.draft.entity.Order;
import ru.app.draft.models.Strategy;
import ru.app.draft.models.User;
import ru.app.draft.models.UserCache;
import ru.app.draft.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.app.draft.store.Store.LAST_PRICE;
import static ru.app.draft.store.Store.USER_STORE;

@Service
public class DbService {

    private final UserRepository userRepository;
    private final StrategyRepository strategyRepository;
    private final OrderRepository orderRepository;
    private final LastPriceRepository lastPriceRepository;
    private final CommentRepository commentRepository;

    public DbService(UserRepository userRepository, StrategyRepository strategyRepository, OrderRepository orderRepository, LastPriceRepository lastPriceRepository, CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.strategyRepository = strategyRepository;
        this.orderRepository = orderRepository;
        this.lastPriceRepository = lastPriceRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public void deleteAll() {
        orderRepository.deleteAll();
        orderRepository.flush();
        strategyRepository.deleteAll();
        strategyRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
        lastPriceRepository.deleteAll();
        lastPriceRepository.flush();
    }

    @Transactional
    public void saveUsers(List<UserCache> list) {
        list.forEach(i -> {
            User userDto = i.getUser();
            ru.app.draft.entity.User userEntity = new ru.app.draft.entity.User();
            userEntity.setChartid(userDto.getChatId());
            userEntity.setEmail(userDto.getEmail());
            userEntity.setLogin(userDto.getLogin());
            userEntity.setPassword(userDto.getPassword());
            if(userDto.getLastVisit()!=null){
                userEntity.setLastVisit(userDto.getLastVisit());
            }
            ru.app.draft.entity.User userSaved = userRepository.save(userEntity);
            List<Strategy> strategyDto = i.getStrategies();
            List<ru.app.draft.entity.Strategy> strategyEntity = new ArrayList<>(strategyDto.size());
            List<Order> ordersEntity=new ArrayList<>();
            strategyDto.forEach(s -> {
                ru.app.draft.entity.Strategy strategy = new ru.app.draft.entity.Strategy();
                strategy.setActive(s.getIsActive());
                strategy.setUsers(userSaved);
                strategy.setIdStrategy(Integer.valueOf(s.getId()));
                strategy.setDirection(s.getDirection());
                strategy.setName(s.getName());
                strategy.setTicker(s.getTicker());
                strategy.setFigi(s.getFigi());
                strategy.setPosition(s.getCurrentPosition());
                strategy.setDescription(s.getDescription());
                strategy.setProducer(s.getProducer());
                strategy.setMinLot(s.getMinLot());
                strategy.setConsumers(s.getConsumer().toString().replaceAll("\\[","").replaceAll("]","").replaceAll(" ",""));
                strategy.setEnterAveragePrice(s.getEnterAveragePrice().toString().replaceAll("\\[","").replaceAll("]","").replaceAll(" ",""));
                s.getOrders().forEach(o->{
                    Order order = Order.builder()
                            .strategy(strategy)
                            .direction(o.getDirection())
                            .price(o.getPrice())
                            .date(o.getDate())
                                    .quantity(o.getQuantity()).build();
                    ordersEntity.add(order);
                });
                strategyEntity.add(strategy);

            });
            strategyRepository.saveAll(strategyEntity);
            orderRepository.saveAll(ordersEntity);
        });

        List<LastPrice> lastPricesEntity = new ArrayList<>();
        LAST_PRICE.forEach((k, v) -> v.getNameSubscriber().forEach(i -> {
            LastPrice lastPrice = new LastPrice();
            lastPrice.setFigi(k);
            lastPrice.setNameSubscriber(i);
            lastPricesEntity.add(lastPrice);
        }));
        lastPriceRepository.saveAll(lastPricesEntity);
    }

    @Transactional
    public void getAllUsers() {
        List<ru.app.draft.entity.User> entities = userRepository.findAll();
        entities.forEach(i -> {
            User user = new User(i.getLogin(), i.getPassword(), i.getEmail(), i.getChartid());
            List<ru.app.draft.entity.Strategy> strategyEntity = i.getStrategies();
            List<Strategy> strategyList = new ArrayList<>(strategyEntity.size());
            strategyEntity.forEach(n -> {
                Strategy strategy = new Strategy(String.valueOf(n.getIdStrategy()), n.getUsers().getLogin(),
                        n.getName(), n.getDirection(), BigDecimal.ZERO, n.getFigi(), n.getTicker(),
                        n.getActive(), null, null, n.getDescription(), n.getMinLot(), n.getProducer());
                strategy.setCurrentPosition(n.getPosition() != null ? n.getPosition() : null);
                strategyList.add(strategy);

                new ru.app.draft.models.Order();
                strategy.setConsumer(List.of(n.getConsumers().split(",")));
                strategy.setEnterAveragePrice(List.of(n.getEnterAveragePrice().split(",")));
                strategy.setOrders(n.getOrders().stream().map(o-> ru.app.draft.models.Order.builder()
                        .price(o.getPrice())
                        .quantity(o.getQuantity())
                        .direction(o.getDirection())
                        .date(o.getDate())
                        .quantity(o.getQuantity())
                        .build()
                ).collect(Collectors.toList()));
            });
            if (user.getLogin().equals("Admin")) {
                user.setIsAdmin(true);
            }
            UserCache userCache = new UserCache(user);
            userCache.setStrategies(strategyList);
            USER_STORE.put(user.getLogin(), userCache);
        });
        List<ru.app.draft.entity.LastPrice> lastPricesEntity = lastPriceRepository.findAll();
        if (!CollectionUtils.isEmpty(lastPricesEntity)) {
            lastPricesEntity.forEach(i -> {
                LAST_PRICE.computeIfAbsent(i.getFigi(), s -> {
                    ru.app.draft.models.LastPrice lastPrice = new ru.app.draft.models.LastPrice(null, null);
                    lastPrice.addSubscriber(i.getNameSubscriber());
                    return lastPrice;
                });
                LAST_PRICE.computeIfPresent(i.getFigi(), (s, lastPrice) -> {
                    lastPrice.addSubscriber(i.getNameSubscriber());
                    return lastPrice;
                });
            });
        }
    }

    public ru.app.draft.entity.Strategy getStrategy(long id) {
        return strategyRepository.findAllById(Collections.singleton(id)).get(0);
    }

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }
}
