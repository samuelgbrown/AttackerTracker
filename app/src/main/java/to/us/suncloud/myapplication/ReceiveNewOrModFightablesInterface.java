package to.us.suncloud.myapplication;

import android.os.Bundle;

import java.io.Serializable;

public interface ReceiveNewOrModFightablesInterface extends Serializable {
    void receiveFightable(Fightable newFightable);
    void receiveFightable(Fightable newFightable, Bundle fighterBundleData);
    void removeFightable(Fightable fightableToDelete);
    void notifyListChanged();
}