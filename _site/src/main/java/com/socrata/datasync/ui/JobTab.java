package com.socrata.datasync.ui;

import com.socrata.datasync.job.JobStatus;

import javax.swing.*;

/**
 * Author: Adrian Laurenzi
 * Date: 9/11/13
 */
public interface JobTab {

    public JPanel getTabPanel();

    public JobStatus runJobNow();

    public void saveJob();

    public String getJobFileLocation();

    public JLabel getJobTabTitleLabel();
}
