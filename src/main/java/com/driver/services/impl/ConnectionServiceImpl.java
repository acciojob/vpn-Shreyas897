package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user=userRepository2.findById(userId).get();
        if(user.isConnected()){
            throw new Exception("Already connected");
        }
        String country1=user.getCountry().getCountryName().toString();
        if(country1.equals(countryName)){
            return user;
        }
        List<ServiceProvider>serviceProviderList=user.getServiceProviderList();
        if(serviceProviderList==null)
            throw new Exception("Unable to connect");
        ServiceProvider serviceProvider=null;
        List<ServiceProvider>serviceProviders=new ArrayList<>();
        for(ServiceProvider x: serviceProviderList){
            List<Country>countryList=x.getCountryList();
            for(Country c:countryList){
                if(c.getCountryName().toString().equals(countryName)){
                    serviceProviders.add(x);
                }
            }
        }
        if(serviceProviders==null){
            throw new Exception("Unable to connect");
        }
        for(ServiceProvider x:serviceProviders){
            if(serviceProvider==null)
                serviceProvider=x;
            else {
                if(x.getId()<serviceProvider.getId())
                    serviceProvider=x;
            }
        }
        String updatedCode="";
        for(Country x:serviceProvider.getCountryList()){
            if(x.getCountryName().toString().equals(countryName)){
                updatedCode=x.getCode();
                break;
            }

        }
        user.setMaskedIp(updatedCode+"."+serviceProvider.getId()+"."+userId);
        Connection connection=new Connection();
        connection.setServiceProvider(serviceProvider);
        connection.setUser(user);
        serviceProvider.getConnectionList().add(connection);
        user.getConnectionList().add(connection);
        user.setConnected(true);
        connectionRepository2.save(connection);
        userRepository2.save(user);
        return user;

    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user =userRepository2.findById(userId).get();
        if(user.getMaskedIp()==null){
            throw new Exception("Already disconnected");
        }
        List<Connection>connectionList=user.getConnectionList();
        for(Connection x:connectionList){
            ServiceProvider serviceProvider=x.getServiceProvider();
            serviceProvider.getConnectionList().remove(x);
            connectionRepository2.deleteById(x.getId());

        }
        user.setConnectionList(new ArrayList<>());
        user.setMaskedIp(null);
        user.setConnected(false);
        userRepository2.save(user);
        return user;

    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User user = userRepository2.findById(senderId).get();
        User user1 = userRepository2.findById(receiverId).get();

        if(user1.getMaskedIp()!=null){
            String str = user1.getMaskedIp();
            String cc = str.substring(0,3); //chopping country code = cc

            if(cc.equals(user.getCountry().getCode()))
                return user;
            else {
                String countryName = "";

                if (cc.equalsIgnoreCase(CountryName.IND.toCode()))
                    countryName = CountryName.IND.toString();
                if (cc.equalsIgnoreCase(CountryName.USA.toCode()))
                    countryName = CountryName.USA.toString();
                if (cc.equalsIgnoreCase(CountryName.JPN.toCode()))
                    countryName = CountryName.JPN.toString();
                if (cc.equalsIgnoreCase(CountryName.CHI.toCode()))
                    countryName = CountryName.CHI.toString();
                if (cc.equalsIgnoreCase(CountryName.AUS.toCode()))
                    countryName = CountryName.AUS.toString();

                User user2 = connect(senderId,countryName);
                if (!user2.isConnected()){
                    throw new Exception("Cannot establish communication");

                }
                else return user2;
            }

        }
        else{
            if(user1.getCountry().equals(user.getCountry())){
                return user;
            }
            String countryName = user1.getCountry().getCountryName().toString();
            User user2 =  connect(senderId,countryName);
            if (!user2.isConnected()){
                throw new Exception("Cannot establish communication");
            }
            else return user2;

        }
    }

}
