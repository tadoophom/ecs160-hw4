package com.ecs160.hw.model;

import com.ecs160.persistence.annotations.Id;
import com.ecs160.persistence.annotations.PersistableField;
import com.ecs160.persistence.annotations.PersistableObject;

@PersistableObject
public class Issue {
    @Id
    @PersistableField
    private String issueId;

    @PersistableField
    private String bug_type;
    @PersistableField
    private String description;
    @PersistableField
    private String filename;
    @PersistableField
    private int line;
    
    public Issue(){}

    public Issue(String bug_type, int line, String description, String filename){
        this(null, bug_type, line, description, filename);
    }

    public Issue(String issueId, String bug_type, int line, String description, String filename){
        this.issueId = issueId;
        this.bug_type = bug_type;
        this.description = description;
        this.filename = filename;
        this.line = line;
    }

   //Getters 
    public String getIssueId() {
        return issueId;
    }

    public String getBugType() {
        return bug_type;
    }
    public String getDescription() {
        return description;
    }
    public String getFilename() {
        return filename;
    }
    public int getLine() {
        return line;
    }
    //Setters
    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public void setBugType(String bugType) {
        this.bug_type = bugType;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public void setLine(int line) {
        this.line = line;
    }

    //Utility Methods
    @Override
    public String toString() {
        return "Issue{" +
                "issueId='" + issueId + '\'' +
                ", bug_type='" + bug_type + '\'' +   
                ", description='" + description + '\'' +
                ", filename='" + filename + '\'' +
                ", line=" + line +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())){
            return false;
        }

        Issue issue = (Issue) obj;
        
        if (line != issue.line) {
            return false;
        }
        if (issueId != null ? !issueId.equals(issue.issueId) : issue.issueId != null) {
            return false;
        }
        if (bug_type != null ? !bug_type.equals(issue.bug_type) : issue.bug_type != null) {
            return false;
        }
        if (description != null ? !description.equals(issue.description) : issue.description != null) {
            return false;
        }
        return (filename != null ? filename.equals(issue.filename) : issue.filename == null);
    }

    @Override
    public int hashCode() {
        int result;
        if (issueId != null) {
            result = issueId.hashCode();
        } else {
            result = 0;
        }
        result = 31 * result + (bug_type != null ? bug_type.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + line;
        return result;
    }
}
