package com.pintu.service.matching;

import com.pintu.entity.DriverTrip;
import com.pintu.entity.PassengerOrder;
import com.pintu.entity.User;

public interface MatchingEngine {
    MatchEvaluation evaluate(PassengerOrder order, DriverTrip trip, User passenger, User driver);
}
