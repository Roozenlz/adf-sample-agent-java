package sample_team.module.comm;

import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.StandardMessage;
import adf.core.agent.communication.standard.bundle.StandardMessagePriority;
import adf.core.agent.communication.standard.bundle.centralized.*;
import adf.core.agent.communication.standard.bundle.information.*;
import adf.core.agent.info.AgentInfo;
import adf.core.agent.info.ScenarioInfo;
import adf.core.agent.info.WorldInfo;
import adf.core.component.communication.CommunicationMessage;
import adf.core.component.communication.MessageCoordinator;
import adf.impl.module.comm.DefaultChannelSubscriber;
import rescuecore2.standard.entities.StandardEntityURN;

import java.util.ArrayList;
import java.util.List;


public class DefaultMessageCoordinator extends MessageCoordinator {

    @Override
    public void coordinate(AgentInfo agentInfo, WorldInfo worldInfo,
                           ScenarioInfo scenarioInfo, MessageManager messageManager,
                           ArrayList<CommunicationMessage> sendMessageList,
                           List<List<CommunicationMessage>> channelSendMessageList) {

        // have different lists for every agent
        ArrayList<CommunicationMessage> policeMessages = new ArrayList<>();
        ArrayList<CommunicationMessage> ambulanceMessages = new ArrayList<>();
        ArrayList<CommunicationMessage> fireBrigadeMessages = new ArrayList<>();

        ArrayList<CommunicationMessage> voiceMessages = new ArrayList<>();

        StandardEntityURN agentType = getAgentType(agentInfo, worldInfo);

        for (CommunicationMessage msg : sendMessageList) {
            if (msg instanceof StandardMessage
                    && !((StandardMessage) msg).isRadio()) {
                voiceMessages.add(msg);
            } else {
                if (msg instanceof MessageBuilding) {
                    fireBrigadeMessages.add(msg);
                } else if (msg instanceof MessageCivilian) {
                    ambulanceMessages.add(msg);
                } else if (msg instanceof MessageRoad) {
                    fireBrigadeMessages.add(msg);
                    ambulanceMessages.add(msg);
                    policeMessages.add(msg);
                } else if (msg instanceof CommandAmbulance) {
                    ambulanceMessages.add(msg);
                } else if (msg instanceof CommandFire) {
                    fireBrigadeMessages.add(msg);
                } else if (msg instanceof CommandPolice) {
                    policeMessages.add(msg);
                } else if (msg instanceof CommandScout) {
                    if (agentType == StandardEntityURN.FIRE_STATION) {
                        fireBrigadeMessages.add(msg);
                    } else if (agentType == StandardEntityURN.POLICE_OFFICE) {
                        policeMessages.add(msg);
                    } else if (agentType == StandardEntityURN.AMBULANCE_CENTRE) {
                        ambulanceMessages.add(msg);
                    }
                } else if (msg instanceof MessageReport) {
                    if (agentType == StandardEntityURN.FIRE_BRIGADE) {
                        fireBrigadeMessages.add(msg);
                    } else if (agentType == StandardEntityURN.POLICE_FORCE) {
                        policeMessages.add(msg);
                    } else if (agentType == StandardEntityURN.AMBULANCE_TEAM) {
                        ambulanceMessages.add(msg);
                    }
                } else if (msg instanceof MessageFireBrigade) {
                    fireBrigadeMessages.add(msg);
                    ambulanceMessages.add(msg);
                    policeMessages.add(msg);
                } else if (msg instanceof MessagePoliceForce) {
                    ambulanceMessages.add(msg);
                    policeMessages.add(msg);
                } else if (msg instanceof MessageAmbulanceTeam) {
                    ambulanceMessages.add(msg);
                    policeMessages.add(msg);
                }
            }
        }

        if (scenarioInfo.getCommsChannelsCount() > 1) {
            // send radio messages if there are more than one communication channel
            int[] channelSize = new int[scenarioInfo.getCommsChannelsCount() - 1];

            setSendMessages(scenarioInfo, StandardEntityURN.POLICE_FORCE, agentInfo,
                    worldInfo, policeMessages, channelSendMessageList, channelSize);
            setSendMessages(scenarioInfo, StandardEntityURN.AMBULANCE_TEAM, agentInfo,
                    worldInfo, ambulanceMessages, channelSendMessageList, channelSize);
            setSendMessages(scenarioInfo, StandardEntityURN.FIRE_BRIGADE, agentInfo,
                    worldInfo, fireBrigadeMessages, channelSendMessageList, channelSize);
        }

        ArrayList<StandardMessage> voiceMessageLowList = new ArrayList<>();
        ArrayList<StandardMessage> voiceMessageNormalList = new ArrayList<>();
        ArrayList<StandardMessage> voiceMessageHighList = new ArrayList<>();

        for (CommunicationMessage msg : voiceMessages) {
            if (msg instanceof StandardMessage) {
                StandardMessage m = (StandardMessage) msg;
                switch (m.getSendingPriority()) {
                    case LOW:
                        voiceMessageLowList.add(m);
                        break;
                    case NORMAL:
                        voiceMessageNormalList.add(m);
                        break;
                    case HIGH:
                        voiceMessageHighList.add(m);
                        break;
                }
            }
        }

        // set the voice channel messages
        channelSendMessageList.get(0).addAll(voiceMessageHighList);
        channelSendMessageList.get(0).addAll(voiceMessageNormalList);
        channelSendMessageList.get(0).addAll(voiceMessageLowList);
    }


    protected int[] getChannelsByAgentType(StandardEntityURN agentType,
                                           AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo,
                                           int channelIndex) {
        int numChannels = scenarioInfo.getCommsChannelsCount() - 1; // 0th channel
        // is the voice
        // channel
        int maxChannelCount = 0;
        boolean isPlatoon = isPlatoonAgent(agentInfo, worldInfo);
        if (isPlatoon) {
            maxChannelCount = scenarioInfo.getCommsChannelsMaxPlatoon();
        } else {
            maxChannelCount = scenarioInfo.getCommsChannelsMaxOffice();
        }
        int[] channels = new int[maxChannelCount];

        for (int i = 0; i < maxChannelCount; i++) {
            channels[i] = DefaultChannelSubscriber.getChannelNumber(agentType, i,
                    numChannels);
        }
        return channels;
    }


    protected boolean isPlatoonAgent(AgentInfo agentInfo, WorldInfo worldInfo) {
        StandardEntityURN agentType = getAgentType(agentInfo, worldInfo);
        if (agentType == StandardEntityURN.FIRE_BRIGADE
                || agentType == StandardEntityURN.POLICE_FORCE
                || agentType == StandardEntityURN.AMBULANCE_TEAM) {
            return true;
        }
        return false;
    }


    protected StandardEntityURN getAgentType(AgentInfo agentInfo,
                                             WorldInfo worldInfo) {
        StandardEntityURN agentType = worldInfo.getEntity(agentInfo.getID())
                .getStandardURN();
        return agentType;
    }


    protected void setSendMessages(ScenarioInfo scenarioInfo,
                                   StandardEntityURN agentType, AgentInfo agentInfo, WorldInfo worldInfo,
                                   List<CommunicationMessage> messages,
                                   List<List<CommunicationMessage>> channelSendMessageList,
                                   int[] channelSize) {
        int channelIndex = 0;
        int[] channels = getChannelsByAgentType(agentType, agentInfo, worldInfo,
                scenarioInfo, channelIndex);
        int channel = channels[channelIndex];
        int channelCapacity = scenarioInfo.getCommsChannelBandwidth(channel);
        // start from HIGH, NORMAL, to LOW
        for (int i = StandardMessagePriority.values().length - 1; i >= 0; i--) {
            for (CommunicationMessage msg : messages) {
                StandardMessage smsg = (StandardMessage) msg;
                if (smsg.getSendingPriority() == StandardMessagePriority.values()[i]) {
                    channelSize[channel - 1] += smsg.getByteArraySize();
                    if (channelSize[channel - 1] > channelCapacity) {
                        channelSize[channel - 1] -= smsg.getByteArraySize();
                        channelIndex++;
                        if (channelIndex < channels.length) {
                            channel = channels[channelIndex];
                            channelCapacity = scenarioInfo.getCommsChannelBandwidth(channel);
                            channelSize[channel - 1] += smsg.getByteArraySize();
                        } else {
                            // if there is no new channel for that message types, just break
                            break;
                        }
                    }
                    channelSendMessageList.get(channel).add(smsg);
                }
            }
        }
    }
}