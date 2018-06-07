package net.epicforce.migrate.ahp.toucb;

/*
 * MigrateApp.java
 *
 * This is the 'main' class which is used to run the Anthill Pro
 * to IBM UrbanCode Build migration.  It provides the user and command
 * line interfaces needed to run the migration.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.javabuilders.BuildResult;
import org.javabuilders.annotations.DoInBackground;
import org.javabuilders.event.BackgroundEvent;
import org.javabuilders.event.CancelStatus;
import org.javabuilders.swing.SwingJavaBuilder;

import net.epicforce.migrate.ahp.Migration;
import net.epicforce.migrate.ahp.exception.MigrateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateApp extends JFrame
{
    private final static Logger LOG = LoggerFactory.getLogger(MigrateApp.class);

    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    // Used by the Swing Java Builder
    private BuildResult     sjResult;

    // To manage our cards (different control panels)
    private CardLayout      cards;

    // Parent panel for our cards, required for next/prev transitions.
    private JPanel          cardpanel;

    // Which step is showing?
    private int             currentPanel = 0;

    // Fields for Anthill Pro
    private String          ahpHost;
    private String          ahpPort;
    private String          ahpUser;
    private String          ahpPassword;
    private String          ahpKeystorePath;
    private String          ahpKeystorePassword;

    // Fields for selecting workflows
    private String          search;
    private String          concurrency = "5";
    private JLabel          loading;
    private JPanel          wfPickerGroup;
    private JPanel          wfPaneGroup;
    private JPanel          wfPickerBox;
    private JPanel          wfPaneBox;

    // Selected workflows
    protected Map<String, JCheckBox>      selectedWorkflows =
                                            new HashMap<String, JCheckBox>();

    // Fields for UCB
    private String          ucbUrl;
    private String          ucbUsername;
    private String          ucbPassword;

    // Fields for progress
    private JPanel          progressPanel;
    private JTable          progressTable;

    // A Migration class for doing workflow queries
    private Migration       ahp = null;

    // Our migration engine for keeping track of thread status, etc.
    protected MigrateEngine   engine = null;

    // For keeping track of migration status
    protected List<MigrateStatus>   status = null;
    protected WorkflowTableModel    model = null;
    protected StatusWorker          worker = null;

    /*****************************************************************
     * ACCESSORS - required for UI.
     ****************************************************************/

    public String getAhpHost()
    {
        return ahpHost;
    }

    public void setAhpHost(String setting)
    {
        ahpHost = setting;
    }

    public String getAhpPort()
    {
        return ahpPort;
    }

    public void setAhpPort(String setting)
    {
        ahpPort = setting;
    }

    public String getAhpUser()
    {
        return ahpUser;
    }

    public void setAhpUser(String setting)
    {
        ahpUser = setting;
    }

    public String getAhpPassword()
    {
        return ahpPassword;
    }

    public void setAhpPassword(String setting)
    {
        ahpPassword = setting;
    }

    public String getAhpKeystorePath()
    {
        return ahpKeystorePath;
    }

    public void setAhpKeystorePath(String setting)
    {
        ahpKeystorePath = setting;
    }

    public String getAhpKeystorePassword()
    {
        return ahpKeystorePassword;
    }

    public void setAhpKeystorePassword(String setting)
    {
        ahpKeystorePassword = setting;
    }

    public void setSearch(String search)
    {
        this.search = search;
    }

    public String getSearch()
    {
        return this.search;
    }

    public String getUcbUrl()
    {
        return this.ucbUrl;
    }

    public void setUcbUrl(String setting)
    {
        this.ucbUrl = setting;
    }

    public String getUcbUsername()
    {
        return this.ucbUsername;
    }

    public void setUcbUsername(String setting)
    {
        this.ucbUsername = setting;
    }

    public String getUcbPassword()
    {
        return this.ucbPassword;
    }

    public void setUcbPassword(String setting)
    {
        this.ucbPassword = setting;
    }

    public String getConcurrency()
    {
        return this.concurrency;
    }

    public void setConcurrency(String concurrency)
    {
        this.concurrency = concurrency;
    }

    /*****************************************************************
     * CONSTRUCTORS
     ****************************************************************/

    public MigrateApp()
    {
        LOG.debug("Initializing MigrateApp");

        // Build our layout from files
        sjResult = SwingJavaBuilder.build(this);

        // Set our custom layout
        wfPickerBox.setLayout(
            new BoxLayout(wfPickerBox, BoxLayout.Y_AXIS)
        );

        wfPaneBox.setLayout(
            new BoxLayout(wfPaneBox, BoxLayout.Y_AXIS)
        );
    }

    /*****************************************************************
     * METHODS
     ****************************************************************/

    /**
     * @param args      Command line parameters
     *
     * Main call just to bootstrap the GUI.
     */
    public static void main(String[] args)
    {
        LOG.debug("Starting up with arguments: {}", args);
        SwingUtilities.invokeLater(new GUI());
    }

    /**
     * Clicking the cancel/exit button will exit the program and shut
     * things down cleanly.
     */
    private void cancel()
    {
        LOG.debug("Exiting application nicely.  Thanks! <3");

        // Do a nice cleanup if ahp is loaded.
        if(ahp != null) {
            ahp.close();
            ahp = null;
        }

        LOG.debug("Now waiting for engine to stop...");

        // Close our threads
        if(engine != null) {
            engine.close();
            engine = null;
        }

        // kill status worker
        if(worker != null) {
            worker.cancel(true);
        }

        setVisible(false);
        dispose();
        LOG.debug("Finished!");
        System.exit(0); // this hangs otherwise, don't care why :P
    }

    /**
     * Next button will flip to the next card.
     */
    private void nextCard()
    {
        LOG.debug("User clicked next.  Current panel: {}", currentPanel);

        // Common error handling
        try {
            // What page are we on?
            switch(currentPanel) {
                case 0:
                    // Do we already have an ahp?  Close if so.
                    if(ahp != null) {
                        ahp.close();
                        ahp = null;
                    }

                    // Try to connect to AHP, error on false.
                    ahp = new Migration(ahpHost, Integer.parseInt(ahpPort),
                                        ahpUser, ahpPassword, ahpKeystorePath,
                                        ahpKeystorePassword);
                    LOG.debug("Successfully constructed a Migration.");
                    break;
                case 1:
                    // Now we have workflows -- check 'em
                    if(selectedWorkflows.size() == 0) {
                        throw new MigrateException(
                            "Please select at least one workflow to migrate."
                        );
                    }

                    LOG.debug("Selected workflows: {}", selectedWorkflows);
                    break;
                case 2:
                    // Connect to UCB

                    // UCB's API doesn't try to 'do' anything until you
                    // start trying to do something.  So you can't just
                    // test the login and go, you've got to query.
                    if(engine != null) {
                        engine.close();
                        engine = null;
                    }

                    // Make sure this is a number
                    int numThreads;

                    try {
                        numThreads = Integer.parseInt(concurrency);

                        if(numThreads < 1) {
                            throw new MigrateException(
                                "At least one thread is required."
                            );
                        }
                    } catch(NumberFormatException e) {
                        throw new MigrateException(
                            "The thread count must be an integer."
                        );
                    }

                    LOG.debug("Concurrency set to {}", numThreads);

                    engine = new MigrateEngine( ahpHost,
                                                ahpPort,
                                                ahpUser,
                                                ahpPassword,
                                                ahpKeystorePath,
                                                ahpKeystorePassword,
                                                ucbUrl,
                                                ucbUsername,
                                                ucbPassword
                    );

                    LOG.debug("Successfully constructed MigrateEngine");

                    // Flip the card and run
                    currentPanel++;
                    cards.next(cardpanel);

                    // We don't want our local ahp instance anymore.
                    if(ahp != null) {
                        ahp.close();
                        ahp = null;
                    }

                    // Make a list of longs for our engine.
                    List<Long> wfIds =
                               new ArrayList<Long>(selectedWorkflows.size());

                    for(String idStr : selectedWorkflows.keySet()) {
                        wfIds.add(Long.parseLong(idStr));
                    }

                    LOG.debug("Starting MigrateEngine ...");

                    // Start our engine
                    engine.start(wfIds, numThreads);

                    // Initialize status
                    status = engine.check();

                    LOG.debug("Initial status: {}", status);

                    // Create our table
                    model = new WorkflowTableModel();
                    progressTable = new JTable(model);

                    progressTable.setAutoCreateColumnsFromModel(true);
                    progressTable.setShowGrid(true);
                    progressTable.setRowSelectionAllowed(false);
                    progressTable.setColumnSelectionAllowed(false);
                    progressTable.setCellSelectionEnabled(false);
                    progressTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

                    progressTable
                        .getColumn(sjResult.getResource("label.progressCol"))
                        .setCellRenderer(new ProgressCellRender());

                    // Scrollbars
                    JScrollPane scroller = new JScrollPane(progressTable);
                    scroller.setVerticalScrollBarPolicy(
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                    );
                    scroller.setHorizontalScrollBarPolicy(
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    );

                    progressPanel.add(scroller);
                    progressTable.setVisible(true);

                    progressPanel.revalidate();
                    progressPanel.repaint();
                    pack();

                    LOG.debug("Starting status worker thread.");

                    // Background thread to check updates.
                    worker = new StatusWorker();
                    worker.execute();

                    return; // no more cards, evar
                default:
            }

            currentPanel++;
            cards.next(cardpanel);
            LOG.debug("Incremented card, finished nextCard handler");
        } catch(NumberFormatException e) {
            LOG.error("Number format exception", e);
            JOptionPane.showMessageDialog(this, "Invalid port provided.");
        } catch(MigrateException e) {
            LOG.error("Migrate Exception", e);
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    /**
     * Previous button will flip to the previous card.
     */
    private void prevCard()
    {
        LOG.debug("User clicked previous button");
        currentPanel--;
        cards.previous(cardpanel);
    }

    /**
     * Do a search.  This runs in the background so it doesn't lock
     * up the UI.
     */
    @DoInBackground(cancelable=false, blocking=false,
                    indeterminateProgress=true)
    private void doSearch(BackgroundEvent evt)
    {
        LOG.debug("Performing background search: {}", search);
        loading.setVisible(true);

        if(ahp == null) {
            LOG.debug("But anthill isn't connected!");
            JOptionPane.showMessageDialog(this,
                                          "You are not connected to Anthill." +
                                          "  Please go to the previous step " +
                                          " and reconnect."
            );
        } else if((search == null) || (search.length() == 0)) {
            LOG.debug("Search string was not provided");
            JOptionPane.showMessageDialog(this, "Search string is required.");
        } else {
            Map<String, Map<String, Long>> wfs = null;

            try {
                wfs = ahp.fetchWorkflowsForProjectName(search, 0);
            } catch(MigrateException e) {
                LOG.debug("Was unable to get workflows", e);
                JOptionPane.showMessageDialog(this,
                                              e.getMessage()
                );

                loading.setVisible(false);
                return;
            }

            LOG.debug("Got workflows: {}", wfs);

            // Clear old checkboxes
            wfPickerBox.removeAll();

            // Iterate and push workflows into select box.
            for(String project : wfs.keySet()) {
                // I did it this way to make it a little more tidy.
                Map<String, Long> w = wfs.get(project);

                if((w == null) || (w.size() == 0)) {
                    // skip -- shouldn't happen though
                    continue;
                }

                // Append in a checkbox for each workflow
                for(Map.Entry<String, Long> ent : w.entrySet()) {
                    JCheckBox cb = new JCheckBox(
                        project + ": " + ent.getKey()
                    );

                    cb.setName(ent.getValue().toString());
                    cb.addItemListener(new CheckBoxSelectListen());

                    wfPickerBox.add(cb);
                }
            }

            // Housekeeping
            wfPickerGroup.setVisible(true);
            wfPaneGroup.setVisible(true);
            wfPickerBox.revalidate();
            wfPickerBox.repaint();
            pack();
        }

        loading.setVisible(false);
        LOG.debug("Finished background search");
    }

    /*****************************************************************
     * SUBCLASSES
     ****************************************************************/

    /**
     * This class implements the GUI 'main' itself, which will in turn
     * delicate to the different panels in the GUI.
     */
    public static class GUI implements Runnable
    {
        /**
         * The structure of this was basically taken from the SwingJavaBuilder
         * tutorials, and is mostly boilerplate.
         */
        public void run()
        {
            // Turn on internationalization
            SwingJavaBuilder.getConfig().addResourceBundle("MigrateApp");

            // Set UI manager
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );

                new MigrateApp().setVisible(true);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class is an item listener for check boxes, for selecting
     * workflows.
     */
    private class CheckBoxSelectListen implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            // Which one was clicked?
            JCheckBox cb = (JCheckBox)e.getItem();

            LOG.debug("User clicked Workflow {}", cb.getName());

            // Simply remove it and move on with our lives if it
            // is already picked.
            if(!selectedWorkflows.containsKey(cb.getName())) {
                JCheckBox newCb = new JCheckBox(cb.getText(), true);
                newCb.setName(cb.getName());
                newCb.addItemListener(new CheckBoxUnselectListen());

                selectedWorkflows.put(cb.getName(), newCb);
                wfPaneBox.add(newCb);
                wfPaneBox.revalidate();
                wfPaneBox.repaint();
            }

            wfPickerBox.remove(cb);
            wfPickerBox.revalidate();
            wfPickerBox.repaint();
            pack();
        }
    }

    /**
     * This class is an item listener for check boxes, for unselecting
     * workflows.
     */
    private class CheckBoxUnselectListen implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            // Which one was clicked?
            JCheckBox cb = (JCheckBox)e.getItem();

            LOG.debug("User unselected workflow {}", cb.getName());

            // Remove it
            selectedWorkflows.remove(cb.getName());
            wfPaneBox.remove(cb);
            wfPaneBox.revalidate();
            wfPaneBox.repaint();
            pack();
        }
    }

    /**
     * This is the 'table model', required to render the workflow
     * progress.
     */
    private class WorkflowTableModel extends AbstractTableModel
    {

        /**
         * This will always be the number of workflows selected.
         *
         * @return number of rows
         */
        @Override
        public int getRowCount()
        {
            return selectedWorkflows.size();
        }

        /**
         * This will be a static number
         *
         * @return number of columns
         */
        @Override
        public int getColumnCount()
        {
            return 3;
        }

        /**
         * This could be made a little more flexible with a map,
         * but given the small number of columns and relative
         * inflexibility, this will do.
         *
         * @param column        Column number 0 to getColumCount()-1
         * @return String column name or empty string if invalid
         *                column.
         */
        @Override
        public String getColumnName(int column)
        {
            switch(column) {
                case 0: return sjResult.getResource("label.workflowCol");
                case 1: return sjResult.getResource("label.progressCol");
                case 2: return sjResult.getResource("label.errorCol");
                default:
            }

            return "";
        }

        /**
         * Gets a value at a given row/column index pair.
         *
         * @param rowIndex          Which row
         * @param columnIndex       Which column
         * @return an appropriate value for the row/column pair
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if(status == null) {
                return "";
            }

            MigrateStatus val = status.get(rowIndex);

            // What we return is based on columnindex
            switch(columnIndex) {
                case 0:
                    return selectedWorkflows.get(String.valueOf(val.workflowId))
                                            .getText();
                case 1:
                    return val.progress;
                case 2:
                    if(val.progress < 100) {
                        return "Running";
                    } else {
                        if(val.errorMessage == null) {
                            return "Complete";
                        } else {
                            return val.errorMessage;
                        }
                    }
            }
            return "";
        }

        /**
         * This is not permitted
         *
         */
        @Override
        public void setValueAt(Object val, int rowIndex, int colIndex)
        {
        }

        /**
         * Disable editing
         *
         * @param row       Doesn't matter
         * @param col       Doesn't matter
         *
         * @returns always false
         */
        @Override
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }
    }

    /**
     * Class for handling the status bars
     */
    private class ProgressCellRender extends JProgressBar
                                     implements TableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(
                                JTable table, Object value,
                                boolean isSelected, boolean hasFocus,
                                int row, int column)
        {
            int progress = 0;

            if (value instanceof Float) {
                progress = Math.round(((Float) value) * 100f);
            } else if (value instanceof Integer) {
                progress = (int) value;
            }

            setValue(progress);
            return this;
        }
    }

    /**
     * Class for updating the status bars
     */
    private class StatusWorker extends SwingWorker<Object, Object> {
        @Override
        protected Object doInBackground()
                  throws Exception
        {
            int numRunning = selectedWorkflows.size();
            int numComplete = 0;

            LOG.debug("StatusWorker is monitoring {} workflows", numRunning);

            while((!isCancelled()) && (numComplete < numRunning)) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    if(isCancelled()) {
                        return null;
                    }
                }

                // Check all progress
                numComplete = 0;
                status = engine.check();

                LOG.debug("Got status: {}", status);

                for(MigrateStatus stat : status) {
                    if(stat.progress == 100) {
                        numComplete++;
                    }
                }

                LOG.debug("Total complete: {}", numComplete);

                // Re-render table.
                progressTable.validate();
                progressTable.repaint();
            }

            return null;
        }
    }
}
