package liquibase.dbdoc;

import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.DatabaseHistoryException;
import liquibase.util.LiquibaseUtil;
import liquibase.util.StringUtils;

import java.io.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public abstract class HTMLWriter {
    protected File outputDir;
    protected Database database;

    public HTMLWriter(File outputDir, Database database) {
        this.outputDir = outputDir;
        this.database = database;
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    protected abstract void writeCustomHTML(Writer fileWriter, Object object, List<Change> changes, Database database) throws IOException;

    private Writer createFileWriter(Object object) throws IOException {
        return new OutputStreamWriter(new FileOutputStream(new File(outputDir, DBDocUtil.toFileName(object.toString().toLowerCase()) + ".html")), LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class).getOutputEncoding());
    }

    public void writeHTML(Object object, List<Change> ranChanges, List<Change> changesToRun, String changeLog) throws IOException, DatabaseHistoryException, DatabaseException {
        Writer fileWriter = createFileWriter(object);


        try {
            fileWriter.append("<html>");
            writeHeader(object, fileWriter);
            fileWriter.append("<body BGCOLOR=\"white\" onload=\"windowTitle();\">");

            fileWriter.append("<H2>").append(createTitle(object)).append("</H2>\n");

            writeBody(fileWriter, object, ranChanges, changesToRun);

            writeFooter(fileWriter, changeLog);

            fileWriter.append("</body>");
            fileWriter.append("</html>");
        } finally {
            fileWriter.close();
        }

    }

    private void writeFooter(Writer fileWriter, String changeLog) throws IOException {
        fileWriter.append("<hr>Generated: ");
        fileWriter.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date()));
        fileWriter.append("<BR>Against: ");
        fileWriter.append(database.toString());
        fileWriter.append("<BR>Change Log: ");
        fileWriter.append(changeLog);
        fileWriter.append("<BR><BR>Generated By: ");
        fileWriter.append("<a href='http://www.dbmanul.org' target='_TOP'>DB-Manul ").append(LiquibaseUtil
        .getBuildVersion()).append("</a>");
    }

    protected void writeBody(Writer fileWriter, Object object, List<Change> ranChanges, List<Change> changesToRun) throws IOException, DatabaseHistoryException, DatabaseException {
        writeCustomHTML(fileWriter, object, ranChanges, database);
        writeChanges("Pending Changes", fileWriter, changesToRun);
        writeChanges("Past Changes", fileWriter, ranChanges);
    }

    protected void writeTable(String title, List<List<String>> cells, Writer fileWriter) throws IOException {
        fileWriter.append("<P>");
        int colspan = 0;
        if (cells.size() == 0) {
            colspan = 0;
        } else {
            colspan = cells.get(0).size();
        }
        fileWriter.append("<TABLE BORDER=\"1\" WIDTH=\"100%\" CELLPADDING=\"3\" CELLSPACING=\"0\" SUMMARY=\"\">\n")
                .append("<TR BGCOLOR=\"#CCCCFF\" CLASS=\"TableHeadingColor\">\n").append("<TD COLSPAN=").append(String.valueOf(colspan)).append("><FONT SIZE=\"+2\">\n").append("<B>").append(title).append("</B></FONT></TD>\n")
                .append("</TR>\n");

        for (List<String> row : cells) {
            fileWriter.append("<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">\n");
            for (String cell : row) {
                writeTD(fileWriter, cell);
            }
            fileWriter.append("</TR>\n");
        }
        fileWriter.append("</TABLE>\n");
    }

    private void writeTD(Writer fileWriter, String filePath) throws IOException {
        fileWriter.append("<TD VALIGN=\"top\">\n");
        fileWriter.append(filePath);
        fileWriter.append("</TD>\n");
    }

    private void writeHeader(Object object, Writer fileWriter) throws IOException {
        String title = createTitle(object);
        fileWriter.append("<head>")
                .append("<title>").append(title).append("</title>")
                .append("<LINK REL =\"stylesheet\" TYPE=\"text/css\" HREF=\"../../stylesheet.css\" TITLE=\"Style\">")
                .append("<SCRIPT type=\"text/javascript\">")
                .append("function windowTitle()")
                .append("{").append("    parent.document.title=\"").append(title.replaceAll("\"", "'")).append("\";")
                .append("}")
                .append("</SCRIPT>")
                .append("</head>");
    }

    protected abstract String createTitle(Object object);

    protected void writeChanges(String title, Writer fileWriter, List<Change> changes) throws IOException, DatabaseHistoryException, DatabaseException {
        fileWriter.append("<p><TABLE BORDER=\"1\" WIDTH=\"100%\" CELLPADDING=\"3\" CELLSPACING=\"0\" SUMMARY=\"\">\n");
        fileWriter.append("<TR BGCOLOR=\"#CCCCFF\" CLASS=\"TableHeadingColor\">\n");
        fileWriter.append("<TD COLSPAN='4'><FONT SIZE=\"+2\">\n");
        fileWriter.append("<B>");
        fileWriter.append(title);
        fileWriter.append("</B></FONT></TD>\n");
        fileWriter.append("</TR>\n");

        ChangeSet lastChangeSet = null;
        if (changes == null || changes.size() == 0) {
            fileWriter.append("<tr><td>None Found</td></tr>");
        } else {
            for (Change change : changes) {
                if (!change.getChangeSet().equals(lastChangeSet)) {
                    lastChangeSet = change.getChangeSet();
                    fileWriter.append("<TR BGCOLOR=\"#EEEEFF\" CLASS=\"TableSubHeadingColor\">\n");
                    writeTD(fileWriter, "<a href='../changelogs/"+DBDocUtil.toFileName(change.getChangeSet().getFilePath())+".html'>"+change.getChangeSet().getFilePath()+"</a>");
                    writeTD(fileWriter, change.getChangeSet().getId());
                    writeTD(fileWriter, "<a href='../authors/"+DBDocUtil.toFileName(change.getChangeSet().getAuthor().toLowerCase())+".html'>"+StringUtils.escapeHtml(change.getChangeSet().getAuthor().toLowerCase())+"</a>");

                    ChangeSet.RunStatus runStatus = database.getRunStatus(change.getChangeSet());
                    if (runStatus.equals(ChangeSet.RunStatus.NOT_RAN)) {
                        String anchor = change.getChangeSet().toString(false).replaceAll("\\W","_");
                        writeTD(fileWriter, "NOT YET RAN [<a href='../pending/sql.html#"+ anchor +"'>SQL</a>]");
                    } else if (runStatus.equals(ChangeSet.RunStatus.INVALID_MD5SUM)) {
                        writeTD(fileWriter, "INVALID MD5SUM");
                    } else if (runStatus.equals(ChangeSet.RunStatus.ALREADY_RAN)) {
                        writeTD(fileWriter, "Executed "+ DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(database.getRanDate(change.getChangeSet())));
                    } else if (runStatus.equals(ChangeSet.RunStatus.RUN_AGAIN)) {
                        writeTD(fileWriter, "Executed, WILL RUN AGAIN");
                    } else {
                        throw new RuntimeException("Unknown run status: "+runStatus);
                    }

                    fileWriter.append("</TR>");

                    if (StringUtils.trimToNull(change.getChangeSet().getComments()) != null) {
                        fileWriter.append("<TR><TD BGCOLOR='#EEEEFF' CLASS='TableSubHeadingColor' colspan='4'>").append(change.getChangeSet().getComments()).append("</TD></TR>");
                    }

                }

                fileWriter.append("<TR BGCOLOR=\"white\" CLASS=\"TableRowColor\">\n");
                fileWriter.append("<td colspan='4'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append(change.getConfirmationMessage()).append("</td></TR>");
            }
        }

        fileWriter.append("</TABLE>");
        fileWriter.append("&nbsp;</P>");        

    }
}
