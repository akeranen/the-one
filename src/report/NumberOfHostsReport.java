package report;


public class NumberOfHostsReport extends Report {

    public NumberOfHostsReport() {}

    public void writeNumberOfHosts(int nrofHosts) { this.write(String.valueOf(nrofHosts)); }
}

