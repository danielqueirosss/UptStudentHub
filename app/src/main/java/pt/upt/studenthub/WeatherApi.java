package pt.upt.studenthub;
/**
 * Mantém a interface do módulo 2, delegando os URLs na classe ExplorerApi.
 */
public final class WeatherApi {
    // Universidade Portucalense, Porto
    public static final double UPT_LATITUDE = 41.18;
    public static final double UPT_LONGITUDE = -8.61;
    private WeatherApi() { }
    /** Devolve o URL da meteorologia atual do campus, como na Semana 7. */
    public static String currentWeatherUrl() {
        return ExplorerApi.campusWeatherUrl();
    }
    /** Devolve um caminho inexistente para demonstrar o tratamento de erros HTTP. */
    public static String brokenUrl() {
        return ExplorerApi.brokenCampusWeatherUrl();
    }
}
