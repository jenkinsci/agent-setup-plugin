package org.jenkinsci.plugins.slave_setup;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
// import hudson.remoting.Launcher;
import hudson.slaves.ComputerListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ComputerListenerImpl extends ComputerListener {


    private SetupDeployer deployer = new SetupDeployer();

    /*
    @Override
    public void preLaunch(Computer c, TaskListener listener) throws IOException, InterruptedException {
        /* 
        TODO: 
        Eliminar completamente aquellas acciones en la que no tenemos acceso al sistema de ficheros
            Sus Vistas, sus parametros etc... No se considera util ejecutar una tarea en el maestro, repetidas veces si alguna ya fue correcta.
        De esta forma guardamos en el sistema de ficheros del esclavo la cache de configuración.acceso
        Esta cache permitira, descartar setups ya existentes en la reconexión de un nodo, o en caso de falla de instalación.
        
        listener.getLogger().println("just before slave " + c.getName() + " gets launched ...");

        SetupConfig config = SetupConfig.get();

        listener.getLogger().println("executing pre-launch scripts ...");
        listener.getLogger().println("status channel " + c.getChannel() + " ...");

        deployer.executePreLaunchScripts(c, config, listener);
    }
    */

    /**
     * Prepares the slave before it gets online by copying the given content in root and executing the configured setup script.
     * @param c the computer to set up
     * @param channel not used
     * @param root the root of the slave
     * @param listener log listener
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        /*
        TODO:
            En caso de suceder algun error, se debe marcar el nodo como desconectado
            el mensaje de desconexoón deberá contener la traza del error.
            De esta forma evitaremos repetir instalaciónes en un esclavo conflictivo.
        */
        hudson.Launcher launch = root.createLauncher(listener);
        try{
            listener.getLogger().println("just before slave " + c.getName() + " gets online ...");

            SetupConfig config = SetupConfig.get();
            ArrayList<String> installedComponents = new ArrayList<String>();
            
            FilePath fp = root.child(Utils.configFileName);
            if(fp.exists())
            {
                listener.getLogger().println(String.format("config file found on %s ", fp.getRemote()));
                installedComponents = Utils.getInstalledComponents(listener, fp);
            }else
            {
                if(Utils.createConfigFile(root, listener))
                {
                    listener.getLogger().println("New configFile created");
                }
            }

            listener.getLogger().println("executing prepare script ...");
            deployer.executePrepareScripts(c, config, listener,installedComponents);

            listener.getLogger().println("setting up slave " + c.getName() + " ...");
            
            deployer.deployToComputer(c, root, listener, config);
            listener.getLogger().println("status channel " + channel + " ..." + root.toString());

            listener.getLogger().println("slave setup done.");
        }catch(Exception ex)
        {
            listener.getLogger().println("Aborting preOnline execution due an error!\nErr: " + ex.getMessage());
            
        }finally
        {
            if(launch.isUnix())
            {
                try{
                    listener.getLogger().println("Launching UNIX ending script.");
                    String script = "find /tmp -type f -atime +10 -delete -iname *jenkins*.sh";
                    deployer.executeScriptOnMaster(script, c, listener);
                    deployer.executeScript(c.getNode(), root, listener, script, deployer.createEnvVarsForComputer(c));
                    listener.getLogger().println("Ending PreOnline methods.");
                    
                }catch(Exception ex)
                {
                    listener.getLogger().println("Error during UNIX ending script: " + ex.getMessage());
                    
                }
            }    

        }
        /*

        Al finalizar si estamos sobre un linux, tanto maestro como exclavo, ejecutaremos esta instrucción para eliminar nuestros temporales (mayores a 10 minutos)
        find /tmp -type f -atime +10 -delete -iname \*jenkins\*.sh
        */
    }

}
