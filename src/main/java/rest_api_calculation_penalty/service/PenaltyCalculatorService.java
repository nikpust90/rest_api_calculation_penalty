package rest_api_calculation_penalty.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rest_api_calculation_penalty.entitity.*;
import rest_api_calculation_penalty.entitity.payment.Payment;
import rest_api_calculation_penalty.entitity.payment.PaymentsCurrentPeriod;
import rest_api_calculation_penalty.validator.PenaltyCalculationRequestValidator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class PenaltyCalculatorService {

    public List<PenaltyRecord> calculatePenalties(PenaltyCalculationRequest request) {

        //monthPeriodStart   ----МесяцПериодНачРасчета
        //
        //currentMonthCycle   ---МесяцТекущегоЦикла

        // Вызов валидатора для проверки входного запроса
        PenaltyCalculationRequestValidator.validate(request);

        // Проверка: Если списки платежей, начальных сальдо или начислений равны null, заменяем их на пустые списки
        List<Payment> payments = Optional.ofNullable(request.getPayments()).orElse(Collections.emptyList());
        List<InitialBalances> initialBalances = Optional.ofNullable(request.getInitialBalances()).orElse(Collections.emptyList());
        List<AccrualOfMonth> accrualOfMonths = Optional.ofNullable(request.getAccrualOfMonths()).orElse(Collections.emptyList());


        log.info("Начало расчета пени для периода {} - {}", request.getPeriodStart(), request.getPeriodEnd());

        List<PenaltyRecord> penaltyRecords = new ArrayList<>();

        LocalDate periodStart = request.getPeriodStart(); //ДатаНачала
        LocalDate periodEng = request.getPeriodEnd();

        //производим расчет пени по каждому лицевому счету
        for (Account account : request.getAccounts()) {
            log.info("Обрабатывается счет: {}", account.getId());
            //получим все оплаты по лицевому счету //НайденныеСтрокиОплатПоЛС - accountPayments
            List<Payment> accountPayments = payments.stream()
                    .filter(p -> p != null && account.getId().equals(p.getAccountId()))
                    .collect(Collectors.toList());

            // Получаем начальный остаток задолженности для счета (if отсутствует – считаем 0)
            InitialBalances initialBalance = initialBalances.stream()
                    .filter(b -> b != null && account.getId().equals(b.getAccountId()))
                    .findFirst()
                    .orElse(new InitialBalances(account.getId(), 0.0));

            // Вычисляем количество месяцев между начальной датой и конечным периодом начисления
            long monthsCount = ChronoUnit.MONTHS.between(periodStart, periodEng) + 1;


            //должны отработать задолженность на начало первого месяца либо начисления за каждый последующий месяц
            for (int i = 0; i <= monthsCount; i++) {

                //Определим период/месяц какой идет по циклу - к примеру 01.01.2025 - первый число месяца
                //здесь мы как бы берем начисление на месяц раньше. То есть мы взяли уже долг на начало периода например на 01.02.2024 а далее нужно взять начисление уже
                //за сам февраль и получается что на 01.03.24 будет начисление февраля
                LocalDate monthPeriodStart = periodStart.plusMonths(i-1).withDayOfMonth(1); //МесяцПериодНачРасчета
                LocalDate monthEnd = monthPeriodStart.withDayOfMonth(monthPeriodStart.lengthOfMonth());
                log.info("Обрабатывается месяц: {} - {} для счета: {}", monthPeriodStart, monthEnd, account.getId());

                // Определяем сумму задолженности
                //totalDebt - сумма задолженности
                double totalDebt = (i == 0)
                        ? initialBalance.getInitialDebt()
                        : accrualOfMonths.stream()
                        .filter(record -> record != null &&
                                account.getId().equals(record.getAccountId()) &&
                                record.getBeginMonth() != null &&
                                record.getEndMonth() != null &&
                                record.getBeginMonth().isEqual(monthPeriodStart) &&
                                record.getEndMonth().isEqual(monthEnd))
                        .mapToDouble(AccrualOfMonth::getAmountAccrual)
                        .findFirst()
                        .orElse(0.0);
                log.debug("Задолженность на начало месяца: {} для счета: {}", totalDebt, account.getId());

                //ЕслиСуммаДолгаМеньшеРавноНуляТоВыведемДефолтнуюСтроку
                if (totalDebt <= 0.0) {
                    log.debug("Задолженность отсутствует, добавляем пустую запись для счета: {}", account.getId());
                    PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), monthPeriodStart.toString(),
                            totalDebt,
                            30, 0.0033333, 0,
                            null, 0,null);
                    penaltyRecords.add(record);
                    continue;

                }

                int limit30 = 30;
                int limit300 = 60;
                int limit130 = 0;
                for (int step = 0; step <= ChronoUnit.MONTHS.between(monthPeriodStart, periodEng); step++) {
                    LocalDate currentMonthCycle = monthPeriodStart.plusMonths(step).withDayOfMonth(1); //МесяцТекущегоЦикла
                    log.debug("Текущий цикл: {}", currentMonthCycle);

                    //if не находим оплату то начсиляем пеню в этом месяце
                    //а так же переходим в следующий месяц
                    if (totalDebt > 0 && limit30 > 0) {

                        //получим оплаты за тек. месяц //ОплатаЗаУказПериод
                        List<PaymentsCurrentPeriod> paymentsCurrentPeriod = findPaymentsPeriod(currentMonthCycle, currentMonthCycle.withDayOfMonth(currentMonthCycle.lengthOfMonth()), accountPayments, periodStart);

                        //if оплаты не было
                        if (paymentsCurrentPeriod == null || paymentsCurrentPeriod.isEmpty()) {

                            PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                    totalDebt,
                                    30, 0.0033333, 0,
                                    null, 0,null);
                            penaltyRecords.add(record);
                            //уменьшаем дни льготного периода на 30 дней
                            limit30 = limit30 - 30;
                            continue;
                        }

                        //если есть оплаты
                        if (paymentsCurrentPeriod != null &&!paymentsCurrentPeriod.isEmpty()) {

                            boolean counter_by_first_advance_one_payment = false; //Счетчик_Уже_Был_Первый_АвансС_Одной_Оплаты

                            for (PaymentsCurrentPeriod payment : paymentsCurrentPeriod) {
                                double totalDebtInitial = totalDebt;
                                totalDebt = totalDebt - payment.getPaymentAmount();
                                log.debug("Оплата: {}, остаток долга: {}", payment.getPaymentAmount(), totalDebt);
                                //if сумма задолженности становится меньше нуля то переносим оплату на другой период
                                if (totalDebt < 0) {
                                    updateSummPayments(totalDebt, payment, accountPayments, counter_by_first_advance_one_payment);//ОбновитьСуммуОплаты
                                    counter_by_first_advance_one_payment = true;
                                    log.debug("Обновлена сумма оплаты, остаток долга: {}", totalDebt);
                                } else {

                                    //удаляем оплаты if мы уже погасили задолженность на всю сумму этой оплаты. Что бы потом можно было выявить суммы авансов образовавшиеся
                                    deleteUsePaymentForTable(payment, accountPayments);
                                    log.debug("Оплата полностью покрыла задолженность, удалена из учета");
                                }

                                //уменьшаем дни льготного периода на разницу
                                limit30 = limit30 - payment.getDaysDifference();

                                if (totalDebtInitial <=0) continue;

                                PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                        totalDebtInitial,
                                        payment.getDaysDifference(), 0.0033333, 0,
                                        payment.getRegistrar(), payment.getPaymentAmount(),payment.getPaymentDate().toString());
                                penaltyRecords.add(record);
                                
                            }

                            //выведем строку с тем что бы добрать дни до конца льгоного периода что бы в дальнейшем он уже не использовался
                            PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                    totalDebt,
                                    limit30, 0.0033333, 0,
                                    null, 0,null);
                            penaltyRecords.add(record);

                            limit30 = 0;
                            
                            continue;

                        }
                    }

                    //расчитываем уже без льготного периода по 1/300  
                    if (totalDebt > 0 && limit30 <= 0 && (limit300 > 0 && limit300 <= 60)) {
                        double totalDebtInitial = 0;
                        //получим оплаты за тек. месяц //ОплатаЗаУказПериод
                        List<PaymentsCurrentPeriod> paymentsCurrentPeriod = findPaymentsPeriod(currentMonthCycle, currentMonthCycle.withDayOfMonth(currentMonthCycle.lengthOfMonth()), accountPayments, periodStart);
                        //if оплаты не было
                        if (paymentsCurrentPeriod == null || paymentsCurrentPeriod.isEmpty()) {

                            log.debug("Начисляется пени по 1/300: {}", account.getId());
                            PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                    totalDebt,
                                    30, 0.0033333, calculatePenalty(totalDebt, 30, findToBetCentralBank(), 0.0033333),
                                    null, 0,null);
                            penaltyRecords.add(record);
                            //уменьшаем дни льготного периода на 30 дней
                            limit300 = limit300 - 30;
                            continue;
                        };

                        //if есть оплаты
                        if (paymentsCurrentPeriod != null &&!paymentsCurrentPeriod.isEmpty()) {

                            boolean counter_by_first_advance_one_payment = false;
                            int DayOfCountRestEndMonth =30; //ДниДляПодсчетаОстаткаДоконцаМес


                            for (PaymentsCurrentPeriod payment : paymentsCurrentPeriod) {
                                totalDebtInitial = totalDebt;
                                totalDebt = totalDebt - payment.getPaymentAmount();
                                //if сумма задолженности становится меньше нуля то переносим оплату на другой период
                                if (totalDebt < 0) {
                                    updateSummPayments(totalDebt, payment, accountPayments, counter_by_first_advance_one_payment);//ОбновитьСуммуОплаты
                                    counter_by_first_advance_one_payment = true;
                                }else {
                                    //удаляем оплаты if мы уже погасили задолженность на всю сумму этой оплаты. Что бы потом можно было выявить суммы авансов образовавшиеся
                                    deleteUsePaymentForTable(payment, accountPayments);
                                } ;

                                //уменьшаем дни льготного периода на разницу
                                limit300 = limit300 - payment.getDaysDifference();
                                DayOfCountRestEndMonth = DayOfCountRestEndMonth - payment.getDaysDifference();

                                if (totalDebtInitial <= 0) continue;
                                log.debug("Начисляется пени по 1/300: {}", account.getId());
                                PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                        totalDebtInitial,
                                        payment.getDaysDifference(), 0.0033333, calculatePenalty(totalDebtInitial, payment.getDaysDifference(), findToBetCentralBank(), 0.0033333),
                                        payment.getRegistrar(), payment.getPaymentAmount(),payment.getPaymentDate().toString());
                                penaltyRecords.add(record);
                                
                            }

                            if (totalDebt <= 0) { break; }

                            if (totalDebt > 0) {
                                log.debug("Начисляется пени по 1/300: {}", account.getId());
                                PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                        totalDebt,
                                        DayOfCountRestEndMonth, 0.0033333, calculatePenalty(totalDebt, DayOfCountRestEndMonth, findToBetCentralBank(), 0.0033333),
                                        null, 0,null);
                                penaltyRecords.add(record);
                                

                                limit300 = limit300 - DayOfCountRestEndMonth;

                                if (limit300 == 0) continue;
                            }
                        }
                    }


                    //расчитываем уже по 1/130   
                    if (totalDebt > 0 && limit30 <= 0 && limit300 <= 0) {
                        double totalDebtInitial = 0;
                        //получим оплаты за тек. месяц
                        List<PaymentsCurrentPeriod> paymentsCurrentPeriod = findPaymentsPeriod(currentMonthCycle, currentMonthCycle.withDayOfMonth(currentMonthCycle.lengthOfMonth()), accountPayments, periodStart);
                        //if оплаты не было
                        if (paymentsCurrentPeriod == null || paymentsCurrentPeriod.isEmpty()) {

                            log.debug("Начисляется пени по 1/130: {}", account.getId());
                            PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                    totalDebt,
                                    30, 0.007692, calculatePenalty(totalDebt, 30, findToBetCentralBank(), 0.007692),
                                    null, 0,null);
                            penaltyRecords.add(record);
                            
                            //уменьшаем дни льготного периода на 30 дней
                            limit130 = limit130 - 30;
                            continue;
                        };

                        //if есть оплаты
                        if (paymentsCurrentPeriod != null &&!paymentsCurrentPeriod.isEmpty()) {


                            boolean counter_by_first_advance_one_payment = false;
                            int DayOfCountRestEndMonth = 30;

                            for (PaymentsCurrentPeriod payment : paymentsCurrentPeriod) {
                                    totalDebtInitial = totalDebt;
                                    totalDebt = totalDebt - payment.getPaymentAmount();
                                    //if сумма задолженности становится меньше нуля то переносим оплату на другой период
                                    if (totalDebt < 0) {
                                        updateSummPayments(totalDebt, payment, accountPayments, counter_by_first_advance_one_payment);//ОбновитьСуммуОплаты
                                        counter_by_first_advance_one_payment = true;
                                    }else {
                                        //удаляем оплаты if мы уже погасили задолженность на всю сумму этой оплаты. Что бы потом можно было выявить суммы авансов образовавшиеся
                                        deleteUsePaymentForTable(payment, accountPayments);
                                    }

                                    if (totalDebtInitial <= 0) continue;

                                    log.debug("Начисляется пени по 1/130: {}", account.getId());
                                    PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                            totalDebtInitial,
                                            payment.getDaysDifference(), 0.007692, calculatePenalty(totalDebtInitial, payment.getDaysDifference(), findToBetCentralBank(), 0.007692),
                                            payment.getRegistrar(), payment.getPaymentAmount(),payment.getPaymentDate().toString());
                                    penaltyRecords.add(record);

                                DayOfCountRestEndMonth = DayOfCountRestEndMonth - payment.getDaysDifference();

                            }

                            if (totalDebt > 0) {
                                log.debug("Начисляется пени по 1/130: {}", account.getId());
                                PenaltyRecord record  = new PenaltyRecord(account.getId(), monthPeriodStart.toString(), currentMonthCycle.toString(),
                                        totalDebt,
                                        DayOfCountRestEndMonth, 0.007692, calculatePenalty(totalDebt, DayOfCountRestEndMonth, findToBetCentralBank(), 0.007692),
                                        null, 0,null);
                                penaltyRecords.add(record);


                            }

                            if (totalDebt <= 0) { break; };
                        }

                    }
                }

            }

        }

        log.info("Расчет пени завершен, всего записей: {}", penaltyRecords.size());
        return penaltyRecords;
    }

    private double findToBetCentralBank() {
        return 0.095;
    }

    private void deleteUsePaymentForTable(PaymentsCurrentPeriod payment, List<Payment> accountPayments) {
        // Проверка: Если входные параметры равны null, выходим из метода
        if (payment == null || accountPayments == null) {
            log.error("Не удалось удалить оплату");
            return;
        }
        // Удаляем оплату из списка, if у неё совпадает регистратор с переданной оплатой
        accountPayments.removeIf(p -> p.getRegistrar().equals(payment.getRegistrar()));
    }

    private void updateSummPayments(double totalDebt, PaymentsCurrentPeriod payment, List<Payment> accountPayments, boolean counterByFirstAdvanceOnePayment) {
        // if сумма задолженности отрицательная
        if (totalDebt < 0 && payment != null && accountPayments != null) {
            for(Payment step : accountPayments) {
                // Ищем строку по регистратору
                if (step.getRegistrar().equals(payment.getRegistrar())) {
                    // Обновляем сумму оплаты, заменяя знак минус на плюс
                    if (counterByFirstAdvanceOnePayment == false) {
                        step.setPaymentAmount(-totalDebt);
                    }
                    break;
                }
            }
        }
    }

    private List<PaymentsCurrentPeriod> findPaymentsPeriod(LocalDate currentMonthCycleStart, LocalDate currentMonthCycleEnd, List<Payment> accountPayments, LocalDate periodStart) {

        // Проверка входных параметров на null
        if (currentMonthCycleStart == null || currentMonthCycleEnd == null || accountPayments == null || periodStart == null) {
            throw new IllegalArgumentException("Один из входных параметров таблицы Платежи текущего периода равен null");
        }

        List<PaymentsCurrentPeriod> paymentsCurrentPeriods = new ArrayList<>();

        // Заполняем таблицу значений, отбирая оплаты которые по сути будут являтся авансом за предыдущие периоды ----> это нужно в дальнейшем когда пойдут периоды в цикле
        paymentsCurrentPeriods.addAll(
                accountPayments.stream()
                        .filter(payment -> !payment.getPaymentDate().isBefore(periodStart) && !payment.getPaymentDate().isAfter(currentMonthCycleStart))
                        .map(payment -> new PaymentsCurrentPeriod(
                                payment.getRegistrar(),
                                currentMonthCycleStart,
                                payment.getPaymentAmount(),
                                true,
                                0))
                        .toList()
        );

        // Отбираем оплаты за сам период
        paymentsCurrentPeriods.addAll(
                accountPayments.stream()
                        .filter(payment -> !payment.getPaymentDate().isBefore(currentMonthCycleStart) && !payment.getPaymentDate().isAfter(currentMonthCycleEnd))
                        .map(payment -> new PaymentsCurrentPeriod(
                                payment.getRegistrar(),
                                payment.getPaymentDate(),
                                payment.getPaymentAmount(),
                                false,
                                0))
                        .toList()
        );

        // Сортируем список оплат по дате
        paymentsCurrentPeriods.sort(Comparator.comparing(PaymentsCurrentPeriod::getPaymentDate));

        // Вычисляем разницу в днях
        final LocalDate[] previousDate = {null}; // Используем массив для изменения значения внутри лямбда-функции
        paymentsCurrentPeriods.forEach(payment -> {
            if (previousDate[0] == null) {
                // Для первой даты считаем разницу от начала месяца
                payment.setDaysDifference(calculateDaysDifference(getMonthStart(payment.getPaymentDate()), payment.getPaymentDate()));
            } else {
                // Для остальных считаем разницу между соседними датами
                payment.setDaysDifference(calculateDaysDifference(previousDate[0], payment.getPaymentDate()));
            }
            previousDate[0] = payment.getPaymentDate();
        });

        // Проверяем, не пуст ли список
        return paymentsCurrentPeriods.isEmpty() ? null : paymentsCurrentPeriods; // Аналогично "Неопределено" в 1С




    }

    // Метод для вычисления разницы в днях
    private static int calculateDaysDifference(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            log.error("Ошибка на null в методе calculateDaysDifference");
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }

    // Метод для получения первого дня месяца
    private static LocalDate getMonthStart(LocalDate date) {
        if (date == null) {
            log.error("Ошибка на null в методе getMonthStart");
            return null;
        }
        return date.withDayOfMonth(1);
    }

    public static double calculatePenalty(double totalDebt, int days, double rate, double bat) {
        return totalDebt * days * rate * bat;
    }

}
