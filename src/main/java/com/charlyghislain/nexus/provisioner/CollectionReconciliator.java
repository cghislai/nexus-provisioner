package com.charlyghislain.nexus.provisioner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CollectionReconciliator {

    private boolean prune;
    private final static Logger LOG = Logger.getLogger(CollectionReconciliator.class.getName());

    public CollectionReconciliator(boolean prune) {
        this.prune = prune;
    }

    public <T, U> void reconcileCollectionsBPreferServer(String resourceMessage,
                                                         List<T> sererValues,
                                                         List<U> configValues,
                                                         Function<T, String> serverIdMapper,
                                                         Function<U, String> configIfMapper,
                                                         Consumer<T> creationConsumer,
                                                         Consumer<T> removalConsumer,
                                                         Consumer<T> updateConsumer,
                                                         Supplier<T> newValueFactory,
                                                         BiConsumer<T, U> converter
    ) {
        Map<String, T> serverNamesValues = sererValues.stream()
                .collect(Collectors.toMap(
                        serverIdMapper,
                        Function.identity()
                ));
        Map<String, U> configNamesValues = configValues.stream()
                .collect(Collectors.toMap(
                        configIfMapper,
                        Function.identity()
                ));


        for (String name : serverNamesValues.keySet()) {
            T serverValue = serverNamesValues.get(name);
            boolean kept = configNamesValues.containsKey(name);
            if (kept) {
                LOG.finer("Updating " + resourceMessage + " " + name);
                U configValue = configNamesValues.get(name);
                converter.accept(serverValue, configValue);
                updateConsumer.accept(serverValue);
                configNamesValues.remove(name);
            } else {
                if (prune) {
                    LOG.info("Pruning " + resourceMessage + " " + name);
                    removalConsumer.accept(serverValue);
                }
            }
        }
        for (String name : configNamesValues.keySet()) {
            LOG.info("Creating " + resourceMessage + " " + name);
            T newValue = newValueFactory.get();
            U configValue = configNamesValues.get(name);
            converter.accept(newValue, configValue);
            creationConsumer.accept(newValue);
        }
    }

    public <API, MODEL> void reconcileCollectionsAPreferModel(String resourceMessage,
                                                              List<API> sererValues,
                                                              List<MODEL> configValues,
                                                              Function<API, String> serverIdMapper,
                                                              Function<MODEL, String> configIfMapper,
                                                              Consumer<MODEL> creationConsumer,
                                                              Consumer<API> removalConsumer,
                                                              Consumer<MODEL> updateConsumer,
                                                              BiConsumer<MODEL, API> patcher
    ) {
        Map<String, API> serverNamesValues = sererValues.stream()
                .collect(Collectors.toMap(
                        serverIdMapper,
                        Function.identity()
                ));
        Map<String, MODEL> configNamesValues = configValues.stream()
                .collect(Collectors.toMap(
                        configIfMapper,
                        Function.identity()
                ));


        for (String name : serverNamesValues.keySet()) {
            API serverValue = serverNamesValues.get(name);
            boolean kept = configNamesValues.containsKey(name);
            if (kept) {
                LOG.finer("Updating " + resourceMessage + " " + name);
                MODEL configValue = configNamesValues.get(name);
                patcher.accept(configValue, serverValue);
                updateConsumer.accept(configValue);
                configNamesValues.remove(name);
            } else {
                if (prune) {
                    LOG.info("Pruning " + resourceMessage + " " + name);
                    removalConsumer.accept(serverValue);
                }
            }
        }
        for (String name : configNamesValues.keySet()) {
            LOG.info("Creating " + resourceMessage + " " + name);
            MODEL configValue = configNamesValues.get(name);
            creationConsumer.accept(configValue);
        }
    }


    public <T, U, V> void reconcileCollectionsA(String resourceMessage,
                                                List<T> sererValues,
                                                List<U> configValues,
                                                Function<T, String> serverIdMapper,
                                                Function<U, String> configIfMapper,
                                                Consumer<V> creationConsumer,
                                                Consumer<V> removalConsumer,
                                                Consumer<V> updateConsumer,
                                                Supplier<V> newValueFactory,
                                                Function<T, U> serverValueConverter,
                                                BiConsumer<V, U> converter
    ) {
        Map<String, T> serverNamesValues = sererValues.stream()
                .collect(Collectors.toMap(
                        serverIdMapper,
                        Function.identity()
                ));
        Map<String, U> configNamesValues = configValues.stream()
                .collect(Collectors.toMap(
                        configIfMapper,
                        Function.identity()
                ));


        for (String name : serverNamesValues.keySet()) {
            T serverValue = serverNamesValues.get(name);
            boolean kept = configNamesValues.containsKey(name);
            if (kept) {
                LOG.finer("Updating " + resourceMessage + " " + name);
                U configValue = configNamesValues.get(name);
                U serverModelValue = serverValueConverter.apply(serverValue);
                V newValue = newValueFactory.get();
                converter.accept(newValue, serverModelValue);
                converter.accept(newValue, configValue);
                updateConsumer.accept(newValue);
                configNamesValues.remove(name);
            } else {
                if (prune) {
                    LOG.info("Pruning " + resourceMessage + " " + name);
                    V newValue = newValueFactory.get();
                    U serverModelValue = serverValueConverter.apply(serverValue);
                    converter.accept(newValue, serverModelValue);
                    removalConsumer.accept(newValue);
                }
            }
        }
        for (String name : configNamesValues.keySet()) {
            LOG.info("Creating " + resourceMessage + " " + name);
            V newValue = newValueFactory.get();
            U configValue = configNamesValues.get(name);
            converter.accept(newValue, configValue);
            creationConsumer.accept(newValue);
        }
    }


    public <T> void reconcileCollectionsA(String resourceMessage,
                                          List<T> sererValues,
                                          List<T> configValues,
                                          Function<T, String> serverIdMapper,
                                          Function<T, String> configIfMapper,
                                          Consumer<T> creationConsumer,
                                          Consumer<T> removalConsumer
    ) {
        Map<String, T> serverNamesValues = sererValues.stream()
                .collect(Collectors.toMap(
                        serverIdMapper,
                        Function.identity()
                ));
        Map<String, T> configNamesValues = configValues.stream()
                .collect(Collectors.toMap(
                        configIfMapper,
                        Function.identity()
                ));


        for (String name : serverNamesValues.keySet()) {
            T serverValue = serverNamesValues.get(name);
            boolean kept = configNamesValues.containsKey(name);
            if (kept) {
                LOG.finer("Updating " + resourceMessage + " " + name);
                configNamesValues.remove(name);
            } else {
                if (prune) {
                    LOG.info("Pruning " + resourceMessage + " " + name);
                    removalConsumer.accept(serverValue);
                }
            }
        }
        for (String name : configNamesValues.keySet()) {
            LOG.info("Creating " + resourceMessage + " " + name);
            T configValue = configNamesValues.get(name);
            creationConsumer.accept(configValue);
        }
    }

    public List<String> reconcileNames(String label, List<String> serverNames, List<String> configNames) {

        Set<String> newNames = new HashSet<>(serverNames);
        reconcileCollectionsA(label, serverNames, configNames, Function.identity(), Function.identity(),
                newNames::add, newNames::remove);
        return new ArrayList<>(newNames).stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }
}
