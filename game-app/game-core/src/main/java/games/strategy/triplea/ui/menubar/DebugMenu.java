package games.strategy.triplea.ui.menubar;

import games.strategy.engine.player.Player;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import lombok.Value;
import org.triplea.swing.SwingAction;

public final class DebugMenu extends JMenu {
  private static final long serialVersionUID = -4876915214715298132L;

  private static final List<Consumer<TripleAFrame>> frameVisitors = new ArrayList<>();

  private static final List<DebugOption> debugOptions = new ArrayList<>();

  private final TripleAFrame frame;

  DebugMenu(final TripleAFrame frame) {
    super("Debug");
    this.frame = frame;

    setMnemonic(KeyEvent.VK_D);

    frameVisitors.forEach(tripleAFrameConsumer -> tripleAFrameConsumer.accept(frame));

    debugOptions.stream()
        .sorted(Comparator.comparing(DebugOption::getName))
        .forEach(
            option -> {
              final JMenu playerDebugMenu = new JMenu(option.getName());
              add(playerDebugMenu);
              renderDebugOption(option.getOptions()).forEach(playerDebugMenu::add);
            });
    if (frameVisitors.isEmpty() && debugOptions.isEmpty()) {
      setVisible(false);
    }
  }

  public static void registerDebugOptions(
      final Player player, final List<AiPlayerDebugOption> options) {
    registerDebugOptions(player.getName(), options);
  }

  public static void registerDebugOptions(
      final String playerName, final List<AiPlayerDebugOption> options) {
    debugOptions.add(new DebugOption(playerName, options));
  }

  public static void registerFrameVisitor(final Consumer<TripleAFrame> visitor) {
    frameVisitors.add(visitor);
  }

  private JMenu renderSubMenuDebugOption(final AiPlayerDebugOption option) {
    final JMenu subMenu = new JMenu(option.getTitle());
    renderDebugOption(option.getSubOptions()).forEach(subMenu::add);
    return subMenu;
  }

  private AiPlayerDebugAction buildDebugAction() {
    return new AiPlayerDebugAction(frame.getMapPanel(), frame.getTerritoryDetails());
  }

  private JMenuItem renderItemDebugOption(
      final AiPlayerDebugOption option, final AiPlayerDebugAction action) {
    final Action swingAction =
        SwingAction.of(option.getTitle(), evt -> option.getActionListener().accept(action));
    switch (option.getOptionType()) {
      case ON_OFF:
        return new JCheckBoxMenuItem(swingAction);
      case ON_OFF_EXCLUSIVE:
        return new JRadioButtonMenuItem(swingAction);
      case NORMAL:
      default:
        return new JMenuItem(swingAction);
    }
  }

  private Collection<JMenuItem> renderDebugOption(final Collection<AiPlayerDebugOption> options) {
    // keep track of all the radio button menu items and their actions so that when one of the
    // radio buttons is selected, the other radio buttons can be deselected
    final Map<String, Map<JMenuItem, AiPlayerDebugAction>> radioButtonDeselectGroups =
        new HashMap<>();
    // each group of radio buttons needs a ButtonGroup so selecting one will deselect the others
    final Map<String, ButtonGroup> radioButtonGroups = new HashMap<>();

    // this stream converts the AiPlayerDebugOption into JMenuItems
    // if the option has sub options, then create a JMenu and recurse to create the sub menu items
    // otherwise, if the option has the actionType == NORMAL, then create a JMenuItem
    // if the option has the actionType == ON_OFF, create a JCheckBoxMenuItem and add a listener
    // to listen for deselection
    // if the option has the actionType == ON_OFF_EXCLUSIVE, create a JRadioButtonMenuItem and
    // add a listener for deselection. Also, add the JRadioButtonMenuItem to a ButtonGroup so that
    // each group of radio buttons only allow one to be selected at a time.
    return options.stream()
        .map(
            option -> {
              if (!option.getSubOptions().isEmpty()) {
                return renderSubMenuDebugOption(option);
              } else {
                final AiPlayerDebugAction debugAction = buildDebugAction();
                final JMenuItem menuItem = renderItemDebugOption(option, debugAction);

                final int mnemonic = option.getMnemonic();
                menuItem.setMnemonic(mnemonic);
                if (option.getOptionType() == AiPlayerDebugOption.OptionType.ON_OFF) {
                  menuItem.addItemListener(
                      e -> {
                        if (e.getStateChange() == ItemEvent.DESELECTED) {
                          debugAction.deselect();
                        }
                      });
                } else if (option.getOptionType()
                    == AiPlayerDebugOption.OptionType.ON_OFF_EXCLUSIVE) {
                  radioButtonDeselectGroups
                      .computeIfAbsent(option.getExclusiveGroup(), k -> new HashMap<>())
                      .put(menuItem, debugAction);
                  radioButtonGroups
                      .computeIfAbsent(option.getExclusiveGroup(), k -> new ButtonGroup())
                      .add(menuItem);
                  // when a radio button is clicked, go through all of the other radio buttons
                  // in the group and deselect them
                  menuItem.addItemListener(
                      e ->
                          radioButtonDeselectGroups
                              .get(option.getExclusiveGroup())
                              .entrySet()
                              .stream()
                              .filter(entry -> !entry.getKey().equals(e.getSource()))
                              .forEach(entry -> entry.getValue().deselect()));
                }
                // ActionType.NORMAL doesn't use deselect so it doesn't need extra logic

                return menuItem;
              }
            })
        .collect(Collectors.toList());
  }

  @Value
  private static class DebugOption {
    String name;
    List<AiPlayerDebugOption> options;
  }
}
