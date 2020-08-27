package to.us.suncloud.myapplication;

public interface MasterCombatantListHolder {
    void combatantListModified(); // Used to notify parents that a Combatant was removed from this list, and the views may need to be laid out again
    AllFactionCombatantLists getMasterCombatantList(); // Get a reference to the master AllFactionCombatantsLists
}
